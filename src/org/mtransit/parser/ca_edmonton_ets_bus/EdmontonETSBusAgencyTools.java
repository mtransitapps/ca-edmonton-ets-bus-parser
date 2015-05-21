package org.mtransit.parser.ca_edmonton_ets_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.edmonton.ca/transportation/ets/about_ets/ets-data-for-developers.aspx
// http://webdocs.edmonton.ca/transit/etsdatafeed/google_transit.zip
public class EdmontonETSBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-edmonton-ets-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new EdmontonETSBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating ETS bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating ETS bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.route_short_name); // using route short name as route ID
	}

	private static final String WEST_EDM_MALL = "West Edm. Mall";
	private static final String WEM_LEWIS_FARMS = WEST_EDM_MALL + " / Lewis Farms";
	private static final String CAPILANO = "Capilano"; // "Capilano Transit Ctr"
	private static final String CROMDALE = "Cromdale";
	private static final String JASPER_PLACE = "Jasper Pl";

	private static final String RLN_SPLIT = " - ";

	private static final String RLN_1 = WEST_EDM_MALL + RLN_SPLIT + CAPILANO;
	private static final String RLN_3 = JASPER_PLACE + RLN_SPLIT + CROMDALE;

	@Override
	public String getRouteLongName(GRoute gRoute) {
		int rsn = Integer.parseInt(gRoute.route_short_name);
		switch (rsn) {
		// @formatter:off
		case 1: return RLN_1;
		case 2: break; // keep original
		case 3: return RLN_3;
		// @formatter:on
		}
		String gRouteLongName = gRoute.route_long_name;
		gRouteLongName = MSpec.cleanStreetTypes(gRouteLongName);
		return MSpec.cleanLabel(gRouteLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "2D3092"; // BLUE (from Wikipedia SVG)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final List<String> ROUTE_1_1ST_STOP_IDS_W_WEST_EDM_MALL = Arrays.asList(new String[] { "1620", "2301", "5101" });
	private static final List<String> ROUTE_1_1ST_STOP_IDS_E_CAPILANO_TRANSIT_CTR = Arrays.asList(new String[] { "5009", "5110" });

	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String firstStopId = getFirstStopId(gtfs, gTrip);
		if (mRoute.id == 1l) {
			if (ROUTE_1_1ST_STOP_IDS_W_WEST_EDM_MALL.contains(firstStopId)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, MDirectionType.WEST.intValue());
				return;
			} else if (ROUTE_1_1ST_STOP_IDS_E_CAPILANO_TRANSIT_CTR.contains(firstStopId)) {
				mTrip.setHeadsignString(CAPILANO, MDirectionType.EAST.intValue());
				return;
			}
		} else if (mRoute.id == 2l) {
			return; // split
		} else if (mRoute.id == 3l) {
			return; // split
		} else if (mRoute.id == 4l) {
			return; // split
		}
		System.out.println("Unexpected trip (unexpected 1st stop ID: " + firstStopId + ") " + gTrip);
		System.exit(-1);
	}

	@Override
	public HashSet<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (mRoute.id == 2l) {
			HashSet<MTrip> mTrips = new HashSet<MTrip>();
			MTrip mTripW = new MTrip(mRoute.id);
			mTripW.setHeadsignDirection(MDirectionType.WEST);
			mTrips.add(mTripW);
			MTrip mTripE = new MTrip(mRoute.id);
			mTripE.setHeadsignDirection(MDirectionType.EAST);
			mTrips.add(mTripE);
			return mTrips;
		} else if (mRoute.id == 3l) {
			HashSet<MTrip> mTrips = new HashSet<MTrip>();
			MTrip mTripW = new MTrip(mRoute.id);
			mTripW.setHeadsignString(JASPER_PLACE, MDirectionType.WEST.intValue());
			mTrips.add(mTripW);
			MTrip mTripE = new MTrip(mRoute.id);
			mTripE.setHeadsignString(CROMDALE, MDirectionType.EAST.intValue());
			mTrips.add(mTripE);
			return mTrips;
		} else if (mRoute.id == 4l) {
			HashSet<MTrip> mTrips = new HashSet<MTrip>();
			MTrip mTripW = new MTrip(mRoute.id);
			mTripW.setHeadsignString(WEM_LEWIS_FARMS, MDirectionType.WEST.intValue());
			mTrips.add(mTripW);
			MTrip mTripE = new MTrip(mRoute.id);
			mTripE.setHeadsignString(CAPILANO, MDirectionType.EAST.intValue());
			mTrips.add(mTripE);
			return mTrips;
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	private static final long TID_ROUTE_2_W = MTrip.getNewId(2l, MDirectionType.WEST.intValue());
	private static final long TID_ROUTE_2_E = MTrip.getNewId(2l, MDirectionType.EAST.intValue());

	private static final List<String> ROUTE_2_BEFORE_AFTER_STOP_IDS = Arrays.asList(new String[] { //
			"1256", "1266", "1336", "1407", "1561", "5003", "5008", "5723", "7902" });
	private static final List<Pair<String, String>> ROUTE_2_BEFORE_AFTER_STOP_IDS_W;
	private static final List<Pair<String, String>> ROUTE_2_BEFORE_AFTER_STOP_IDS_E;
	private static final List<Pair<String, String>> ROUTE_2_BEFORE_AFTER_STOP_IDS_BOTH_E_W;
	private static final List<Pair<String, String>> ROUTE_2_BEFORE_AFTER_STOP_IDS_BOTH_W_E;
	static {
		List<Pair<String, String>> list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("7902", "5723"));
		list.add(new Pair<String, String>("7902", StringUtils.EMPTY));
		list.add(new Pair<String, String>(StringUtils.EMPTY, "5723"));
		list.add(new Pair<String, String>("1266", "5003"));
		list.add(new Pair<String, String>("1407", "5003"));
		ROUTE_2_BEFORE_AFTER_STOP_IDS_W = list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("5723", "7902"));
		list.add(new Pair<String, String>("5723", StringUtils.EMPTY));
		list.add(new Pair<String, String>(StringUtils.EMPTY, "7902"));
		list.add(new Pair<String, String>("5008", "1256"));
		list.add(new Pair<String, String>("5008", "1336"));
		list.add(new Pair<String, String>("5008", "1561"));
		ROUTE_2_BEFORE_AFTER_STOP_IDS_E = list;
		list = new ArrayList<Pair<String, String>>();
		ROUTE_2_BEFORE_AFTER_STOP_IDS_BOTH_E_W = list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("7902", "7902")); // 5723
		list.add(new Pair<String, String>("1561", "1336")); // 5723
		list.add(new Pair<String, String>("1561", "5008")); // 5723
		list.add(new Pair<String, String>("5003", "1561")); // 5723
		ROUTE_2_BEFORE_AFTER_STOP_IDS_BOTH_W_E = list;
	}

	private static final long TID_ROUTE_3_W_JASPER_PLACE = MTrip.getNewId(3l, MDirectionType.WEST.intValue());
	private static final long TID_ROUTE_3_E_CROMDALE = MTrip.getNewId(3l, MDirectionType.EAST.intValue());

	private static final List<String> ROUTE_3_BEFORE_AFTER_STOP_IDS = Arrays.asList(new String[] { "1147", "5106" });
	private static final List<Pair<String, String>> ROUTE_3_BEFORE_AFTER_STOP_IDS_W_CROMDALE;
	private static final List<Pair<String, String>> ROUTE_3_BEFORE_AFTER_STOP_IDS_E_JASPER_PLACE;
	private static final List<Pair<String, String>> ROUTE_3_BEFORE_AFTER_STOP_IDS_BOTH_E_JASPER_PLACE_W_CROMDALE;
	private static final List<Pair<String, String>> ROUTE_3_BEFORE_AFTER_STOP_IDS_BOTH_W_CROMDALE_E_JASPER_PLACE;
	static {
		List<Pair<String, String>> list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("5106", StringUtils.EMPTY));
		list.add(new Pair<String, String>("5106", "1147"));
		list.add(new Pair<String, String>(StringUtils.EMPTY, "1147"));
		ROUTE_3_BEFORE_AFTER_STOP_IDS_W_CROMDALE = list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("1147", StringUtils.EMPTY));
		list.add(new Pair<String, String>("1147", "5106"));
		list.add(new Pair<String, String>(StringUtils.EMPTY, "5106"));
		ROUTE_3_BEFORE_AFTER_STOP_IDS_E_JASPER_PLACE = list;
		list = new ArrayList<Pair<String, String>>();
		ROUTE_3_BEFORE_AFTER_STOP_IDS_BOTH_E_JASPER_PLACE_W_CROMDALE = list;
		list = new ArrayList<Pair<String, String>>();
		ROUTE_3_BEFORE_AFTER_STOP_IDS_BOTH_W_CROMDALE_E_JASPER_PLACE = list;
	}

	private static final long TID_ROUTE_4_W_WEM_LEWIS_FARMS = MTrip.getNewId(4l, MDirectionType.WEST.intValue());
	private static final long TID_ROUTE_4_E_CAPILANO = MTrip.getNewId(4l, MDirectionType.EAST.intValue());

	private static final List<String> ROUTE_4_BEFORE_AFTER_STOP_IDS = Arrays.asList(new String[] { "2001", "2002", "2159", "2306", "2447", "2549", "5003",
			"5006", "8601" });
	private static final List<Pair<String, String>> ROUTE_4_BEFORE_AFTER_STOP_IDS_W_WEM_LEWIS_FARMS;
	private static final List<Pair<String, String>> ROUTE_4_BEFORE_AFTER_STOP_IDS_E_CAPILANO;
	private static final List<Pair<String, String>> ROUTE_4_BEFORE_AFTER_STOP_IDS_BOTH_E_CAPILANO_W_WEM_LEWIS_FARMS;
	private static final List<Pair<String, String>> ROUTE_4_BEFORE_AFTER_STOP_IDS_BOTH_W_WEM_LEWIS_FARMS_E_CAPILANO;
	static {
		List<Pair<String, String>> list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("2306", "8601"));
		list.add(new Pair<String, String>("2306", StringUtils.EMPTY));
		list.add(new Pair<String, String>(StringUtils.EMPTY, "8601"));
		list.add(new Pair<String, String>("2159", "5006"));
		list.add(new Pair<String, String>("2002", "5006"));
		list.add(new Pair<String, String>("2159", "2001"));
		ROUTE_4_BEFORE_AFTER_STOP_IDS_W_WEM_LEWIS_FARMS = list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("8601", "2306"));
		list.add(new Pair<String, String>("8601", StringUtils.EMPTY));
		list.add(new Pair<String, String>(StringUtils.EMPTY, "2306"));
		list.add(new Pair<String, String>("5006", "2447"));
		list.add(new Pair<String, String>("5006", "2002"));
		list.add(new Pair<String, String>("5006", "2549"));
		ROUTE_4_BEFORE_AFTER_STOP_IDS_E_CAPILANO = list;
		list = new ArrayList<Pair<String, String>>();
		list.add(new Pair<String, String>("5006", "5006"));
		list.add(new Pair<String, String>("5006", "5003"));
		ROUTE_4_BEFORE_AFTER_STOP_IDS_BOTH_E_CAPILANO_W_WEM_LEWIS_FARMS = list;
		list = new ArrayList<Pair<String, String>>();
		ROUTE_4_BEFORE_AFTER_STOP_IDS_BOTH_W_WEM_LEWIS_FARMS_E_CAPILANO = list;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, HashSet<MTrip> splitTrips, GSpec gtfs) {
		if (mRoute.id == 2l) {
			List<Pair<String, String>> stopIdsTowards2 = ROUTE_2_BEFORE_AFTER_STOP_IDS_W;
			List<Pair<String, String>> stopIdsTowards1 = ROUTE_2_BEFORE_AFTER_STOP_IDS_E;
			List<Pair<String, String>> stopIdsTowardsBoth21 = ROUTE_2_BEFORE_AFTER_STOP_IDS_BOTH_E_W;
			List<Pair<String, String>> stopIdsTowardsBoth12 = ROUTE_2_BEFORE_AFTER_STOP_IDS_BOTH_W_E;
			List<String> allBeforeAfterStopIds = ROUTE_2_BEFORE_AFTER_STOP_IDS;
			long tidTowardsStop1 = TID_ROUTE_2_E;
			long tidTowardsStop2 = TID_ROUTE_2_W;
			return splitTripStop(gTrip, gTripStop, gtfs, stopIdsTowards2, stopIdsTowards1, stopIdsTowardsBoth21, stopIdsTowardsBoth12, tidTowardsStop1,
					tidTowardsStop2, allBeforeAfterStopIds);
		} else if (mRoute.id == 3l) {
			List<Pair<String, String>> stopIdsTowards2 = ROUTE_3_BEFORE_AFTER_STOP_IDS_E_JASPER_PLACE;
			List<Pair<String, String>> stopIdsTowards1 = ROUTE_3_BEFORE_AFTER_STOP_IDS_W_CROMDALE;
			List<Pair<String, String>> stopIdsTowardsBoth21 = ROUTE_3_BEFORE_AFTER_STOP_IDS_BOTH_E_JASPER_PLACE_W_CROMDALE;
			List<Pair<String, String>> stopIdsTowardsBoth12 = ROUTE_3_BEFORE_AFTER_STOP_IDS_BOTH_W_CROMDALE_E_JASPER_PLACE;
			List<String> allBeforeAfterStopIds = ROUTE_3_BEFORE_AFTER_STOP_IDS;
			long tidTowardsStop1 = TID_ROUTE_3_E_CROMDALE;
			long tidTowardsStop2 = TID_ROUTE_3_W_JASPER_PLACE;
			return splitTripStop(gTrip, gTripStop, gtfs, stopIdsTowards2, stopIdsTowards1, stopIdsTowardsBoth21, stopIdsTowardsBoth12, tidTowardsStop1,
					tidTowardsStop2, allBeforeAfterStopIds);
		} else if (mRoute.id == 4l) {
			List<Pair<String, String>> stopIdsTowards2 = ROUTE_4_BEFORE_AFTER_STOP_IDS_W_WEM_LEWIS_FARMS;
			List<Pair<String, String>> stopIdsTowards1 = ROUTE_4_BEFORE_AFTER_STOP_IDS_E_CAPILANO;
			List<Pair<String, String>> stopIdsTowardsBoth21 = ROUTE_4_BEFORE_AFTER_STOP_IDS_BOTH_E_CAPILANO_W_WEM_LEWIS_FARMS;
			List<Pair<String, String>> stopIdsTowardsBoth12 = ROUTE_4_BEFORE_AFTER_STOP_IDS_BOTH_W_WEM_LEWIS_FARMS_E_CAPILANO;
			List<String> allBeforeAfterStopIds = ROUTE_4_BEFORE_AFTER_STOP_IDS;
			long tidTowardsStop1 = TID_ROUTE_4_E_CAPILANO;
			long tidTowardsStop2 = TID_ROUTE_4_W_WEM_LEWIS_FARMS;
			return splitTripStop(gTrip, gTripStop, gtfs, stopIdsTowards2, stopIdsTowards1, stopIdsTowardsBoth21, stopIdsTowardsBoth12, tidTowardsStop1,
					tidTowardsStop2, allBeforeAfterStopIds);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, gtfs);
	}

	private Pair<Long[], Integer[]> splitTripStop(GTrip gTrip, GTripStop gTripStop, GSpec gtfs, List<Pair<String, String>> stopIdsTowards2,
			List<Pair<String, String>> stopIdsTowards1, List<Pair<String, String>> stopIdsTowardsBoth21, List<Pair<String, String>> stopIdsTowardsBoth12,
			long tidTowardsStop1, long tidTowardsStop2, List<String> allBeforeAfterStopIds) {
		Pair<String, String> beforeAfter = getBeforeAfterStopId(gtfs, gTrip, gTripStop, stopIdsTowards2, stopIdsTowards1, stopIdsTowardsBoth21,
				stopIdsTowardsBoth12, allBeforeAfterStopIds);
		if (stopIdsTowards2.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2 }, new Integer[] { gTripStop.stop_sequence });
		} else if (stopIdsTowards1.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1 }, new Integer[] { gTripStop.stop_sequence });
		} else if (stopIdsTowardsBoth21.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2, tidTowardsStop1 }, new Integer[] { 1, gTripStop.stop_sequence });
		} else if (stopIdsTowardsBoth12.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1, tidTowardsStop2 }, new Integer[] { 1, gTripStop.stop_sequence });
		}
		System.out.println("Unexptected trip stop to split " + gTripStop);
		System.exit(-1);
		return null;
	}

	private String getFirstStopId(GSpec gtfs, GTrip gTrip) {
		int gStopMaxSequence = -1;
		String gStopId = null;
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (gStopTime.stop_sequence > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.stop_sequence;
			}
			if (gStopTime.stop_sequence != 1) {
				continue;
			}
			gStopId = gStopTime.stop_id;
		}
		if (StringUtils.isEmpty(gStopId)) {
			System.out.println("Unexpected trip (no 1st stop) " + gTrip);
			System.exit(-1);
		}
		return gStopId;
	}

	private String getLastStopId(GSpec gtfs, GTrip gTrip) {
		int gStopMaxSequence = -1;
		String gStopId = null;
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (gStopTime.stop_sequence < gStopMaxSequence) {
				continue;
			}
			gStopMaxSequence = gStopTime.stop_sequence;
			gStopId = gStopTime.stop_id;
		}
		if (StringUtils.isEmpty(gStopId)) {
			System.out.println("Unexpected trip (no last stop) " + gTrip);
			System.exit(-1);
		}
		return gStopId;
	}

	private Pair<String, String> getBeforeAfterStopId(GSpec gtfs, GTrip gTrip, GTripStop gTripStop, List<Pair<String, String>> stopIdsTowards2,
			List<Pair<String, String>> stopIdsTowards1, List<Pair<String, String>> stopIdsTowardsBoth21, List<Pair<String, String>> stopIdsTowardsBoth12,
			List<String> allBeforeAfterStopIds) {
		int gStopMaxSequence = -1;
		ArrayList<String> afterStopIds = new ArrayList<String>();
		ArrayList<Integer> afterStopSequence = new ArrayList<Integer>();
		ArrayList<String> beforeStopIds = new ArrayList<String>();
		ArrayList<Integer> beforeStopSequence = new ArrayList<Integer>();
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (allBeforeAfterStopIds.contains(gStopTime.stop_id)) {
				if (gStopTime.stop_sequence < gTripStop.stop_sequence) {
					beforeStopIds.add(gStopTime.stop_id);
					beforeStopSequence.add(gStopTime.stop_sequence);
				}
				if (gStopTime.stop_sequence > gTripStop.stop_sequence) {
					afterStopIds.add(gStopTime.stop_id);
					afterStopSequence.add(gStopTime.stop_sequence);
				}
			}
			if (gStopTime.stop_sequence > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.stop_sequence;
			}
		}
		if (allBeforeAfterStopIds.contains(gTripStop.stop_id)) {
			if (gTripStop.stop_sequence == 1) {
				beforeStopIds.add(gTripStop.stop_id);
				beforeStopSequence.add(gTripStop.stop_sequence);
			}
			if (gTripStop.stop_sequence == gStopMaxSequence) {
				afterStopIds.add(gTripStop.stop_id);
				afterStopSequence.add(gTripStop.stop_sequence);
			}
		}
		Pair<String, String> beforeAfterStopIdCurrent;
		Pair<Integer, Pair<String, String>> beforeAfterStopIdCandidate = null;
		String beforeStopId, afterStopId;
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				beforeAfterStopIdCurrent = new Pair<String, String>(beforeStopId, afterStopId);
				if (stopIdsTowards2.contains(beforeAfterStopIdCurrent) || stopIdsTowards1.contains(beforeAfterStopIdCurrent)) {
					int size = Math.max(afterStopSequence.get(a) - gTripStop.stop_sequence, gTripStop.stop_sequence - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, Pair<String, String>>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = new Pair<String, String>(beforeStopId, StringUtils.EMPTY);
			if (stopIdsTowards2.contains(beforeAfterStopIdCurrent) || stopIdsTowards1.contains(beforeAfterStopIdCurrent)) {
				int size = gTripStop.stop_sequence - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, Pair<String, String>>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = new Pair<String, String>(StringUtils.EMPTY, afterStopId);
			if (stopIdsTowards2.contains(beforeAfterStopIdCurrent) || stopIdsTowards1.contains(beforeAfterStopIdCurrent)) {
				int size = afterStopSequence.get(a) - gTripStop.stop_sequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, Pair<String, String>>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				if (gTripStop.stop_id.equals(beforeStopId) && gTripStop.stop_id.equals(afterStopId)) {
					continue;
				}
				beforeAfterStopIdCurrent = new Pair<String, String>(beforeStopId, afterStopId);
				if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
					int size = Math.max(afterStopSequence.get(a) - gTripStop.stop_sequence, gTripStop.stop_sequence - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, Pair<String, String>>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = new Pair<String, String>(beforeStopId, StringUtils.EMPTY);
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				int size = gTripStop.stop_sequence - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, Pair<String, String>>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = new Pair<String, String>(StringUtils.EMPTY, afterStopId);
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				int size = afterStopSequence.get(a) - gTripStop.stop_sequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, Pair<String, String>>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		if (beforeAfterStopIdCandidate != null) {
			return beforeAfterStopIdCandidate.second;
		}
		System.out.println("Unexpected trip (befores:" + beforeStopIds + "|afters:" + afterStopIds + ") " + gTrip);
		System.exit(-1);
		return null;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return MSpec.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = MSpec.cleanStreetTypes(gStopName);
		gStopName = MSpec.cleanNumbers(gStopName);
		return MSpec.cleanLabel(gStopName);
	}
}
