package de.mobanisto.gartendaten.pages.other;

import static de.mobanisto.gartendaten.Fit.GOOD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import de.mobanisto.gartendaten.Data;
import de.mobanisto.gartendaten.Licht;
import de.mobanisto.gartendaten.Plant;
import de.mobanisto.gartendaten.Website;
import de.mobanisto.gartendaten.pages.base.SimpleBaseGenerator;
import de.mobanisto.gartendaten.util.PlantUtil;
import de.topobyte.jsoup.HTML;
import de.topobyte.jsoup.bootstrap4.Bootstrap;
import de.topobyte.jsoup.bootstrap4.components.ContextualType;
import de.topobyte.jsoup.bootstrap4.components.ListGroupDiv;
import de.topobyte.jsoup.bootstrap4.components.listgroup.ListGroupA;
import de.topobyte.jsoup.bootstrap4.components.listgroup.ListGroupItem;
import de.topobyte.webgun.exceptions.PageNotFoundException;
import de.topobyte.webpaths.WebPath;

public class ListeGenerator extends SimpleBaseGenerator
{

	private String listName;
	private Data data;
	private PlantUtil pu;

	public ListeGenerator(WebPath path, String listName)
	{
		super(path);
		this.listName = listName;
		data = Website.INSTANCE.getData();
		pu = new PlantUtil(data);
	}

	@Override
	protected void content()
	{
		content.ac(HTML.h1("Liste"));

		ListGroupDiv list = content.ac(Bootstrap.listGroupDiv());

		List<String> names = data.getNames().get(listName);
		if (names == null) {
			throw new PageNotFoundException();
		}

		Map<String, Plant> plantLookup = data.getPlants();

		List<Plant> plants = new ArrayList<>();

		for (String plantName : names) {
			Plant plant = plantLookup.get(plantName);
			ListGroupA item = list.addA("/pflanze/" + plantName, plantName);
			if (plant == null) {
				item.setContext(ContextualType.DANGER);
			} else {
				plants.add(plant);
				item.setContext(ContextualType.SUCCESS);
			}
		}

		buildGroups(plants);
		display(plants);
	}

	private Map<Integer, Set<Set<Plant>>> sizeToGroups = new HashMap<>();
	private int maxGroupSize;

	private void buildGroups(List<Plant> plants)
	{
		Set<Set<Plant>> groups = new HashSet<>();
		for (Plant plant : plants) {
			Set<Plant> group = new HashSet<>();
			group.add(plant);
			groups.add(group);
		}

		int groupSize = 1;
		while (true) {
			groupSize += 1;
			Set<Set<Plant>> removedGroups = new HashSet<>();
			Set<Set<Plant>> newGroups = new HashSet<>();
			for (Set<Plant> group : groups) {
				boolean extended = false;
				for (Plant plant : plants) {
					if (group.contains(plant)) {
						continue;
					}
					boolean allGood = true;
					for (Plant in : group) {
						if (!match(plant, in)) {
							allGood = false;
							break;
						}
					}
					if (allGood) {
						newGroups.add(extend(group, plant));
						extended = true;
					}
				}
				if (extended) {
					removedGroups.add(group);
				}
			}
			groups.removeAll(removedGroups);
			groups = newGroups;
			if (groups.isEmpty()) {
				break;
			}
			sizeToGroups.put(groupSize, groups);
		}

		maxGroupSize = groupSize - 1;
	}

	private void display(List<Plant> plants)
	{
		Set<Plant> got = new HashSet<>();

		for (int i = maxGroupSize; i >= 2; i--) {
			content.ac(HTML.h2("Gruppen der Größe " + i)).addClass("mt-3");
			ListGroupDiv list = content.ac(Bootstrap.listGroupDiv());
			Set<Set<Plant>> grps = sizeToGroups.get(i);
			for (Set<Plant> group : grps) {
				ListGroupItem item = list.addTextItem(Joiner.on(", ")
						.join(Iterables.transform(group, Plant::getName)));
				item.ap(HTML.br()).at(Joiner.on(", ")
						.join(Iterables.transform(group, pu::getLicht)));
				item.ap(HTML.br()).at("Vorzucht: ").at(Joiner.on(", ")
						.join(Iterables.transform(group, pu::getVorzucht)));
				item.ap(HTML.br()).at("Direktsaat: ").at(Joiner.on(", ")
						.join(Iterables.transform(group, pu::getDirektsaat)));
				for (Plant plant : group) {
					got.add(plant);
				}
			}
		}

		content.ac(HTML.h2("Übrig")).addClass("mt-3");
		ListGroupDiv list = content.ac(Bootstrap.listGroupDiv());
		for (Plant plant : plants) {
			if (got.contains(plant)) {
				continue;
			}
			list.addA("/pflanze/" + plant.getName(), plant.getName());
		}
	}

	private boolean match(Plant a, Plant b)
	{
		if (data.getMix().get(a, b) != GOOD) {
			return false;
		}
		boolean lichtMatch = false;
		Collection<Licht> lichtA = data.getLicht().get(a.getName());
		Collection<Licht> lichtB = data.getLicht().get(b.getName());
		outer: for (Licht lA : lichtA) {
			for (Licht lB : lichtB) {
				if (match(lA, lB)) {
					lichtMatch = true;
					break outer;
				}
			}
		}
		return lichtMatch;
	}

	private boolean match(Licht lA, Licht lB)
	{
		if (lA == lB) {
			return true;
		}
		return false;
	}

	private Set<Plant> extend(Set<Plant> group, Plant plant)
	{
		Set<Plant> result = new HashSet<>(group);
		result.add(plant);
		return result;
	}

}
