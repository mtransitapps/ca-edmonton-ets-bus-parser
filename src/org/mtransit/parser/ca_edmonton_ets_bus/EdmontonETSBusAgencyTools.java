package org.mtransit.parser.ca_edmonton_ets_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://data.edmonton.ca/
// http://www.edmonton.ca/ets/ets-data-for-developers.aspx
// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Schedules-zipped-files/urjq-fvmq
// https://drive.google.com/uc?id=1KcQixzJcucT5PDOwFJBXhDg-Alh0SVP6&export=download
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
		System.out.printf("\nGenerating ETS bus data...");
		long start = System.currentTimeMillis();
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		if (isNext) {
			setupNext();
		}
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating ETS bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void setupNext() {
		ALL_ROUTE_TRIPS2.put(560L, new RouteTripSpec(560L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM, // DOWNTOWN
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8584", // Jennifer Heil Way & TransAlta Tri Leisure Centre #SpruceGrove
								"8761", // Century Road & Grove Drive
								"8910", // == Century Road & Vanderbilt Common
								"5415", // != 154 Street & 119 Avenue =>
								"1223", // ==
								"1226", // !=
								"1113", // <> Kingsway RAH Transit Centre =>
								"1989", // <> 108 Street & 104 Avenue #Edmonton =>
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1113", // <> Kingsway RAH Transit Centre <=
								"1989", // <> 108 Street & 104 Avenue #Edmonton <=
								"1899", // !=
								"1227", // ==
								"5389", // != 154 Street & 118 Avenue <=
								"8371", // Century Road & Kings Link ==
								"8584", // Jennifer Heil Way & TransAlta Tri Leisure Centre #SpruceGrove
						})) //
				.compileBothTripSort());
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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
	public boolean excludeRoute(GRoute gRoute) {
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if ("Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
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
		return Long.parseLong(getRouteShortName(gRoute)); // using route short name as route ID
	}

	private static final String _SLASH_ = " / ";
	private static final String _AND_ = " & ";
	private static final String _AVE = " Ave";
	private static final String _ST = " St";
	private static final String TRANSIT_CENTER_SHORT = "TC";
	private static final String EDM = "Edm";
	private static final String WEST_EDM_MALL = "West " + EDM + " Mall"; // TODO ? "WEM"
	private static final String LEWIS_FARMS = "Lewis Farms";
	private static final String CAPILANO = "Capilano"; //
	private static final String CLAREVIEW = "Clareview";
	private static final String CROMDALE = "Cromdale";
	private static final String JASPER_PLACE = "Jasper Pl";
	private static final String COLISEUM = "Coliseum";
	private static final String WESTMOUNT = "Westmount";
	private static final String UNIVERSITY = "University";
	private static final String MILL_WOODS = "Mill Woods";
	private static final String MILL_WOODS_TC = MILL_WOODS + " TC";
	private static final String NAIT = "NAIT";
	private static final String SOUTHGATE = "Southgate";
	private static final String NORTHGATE = "Northgate";
	private static final String ABBOTTSFIELD = "Abbottsfield";
	private static final String EAUX_CLAIRES = "Eaux Claires";
	private static final String DOWNTOWN = "Downtown";
	private static final String MILLGATE = "Millgate";
	private static final String GOV_CTR = "Government Ctr"; // TODO "Gov Ctr";
	private static final String CASTLE_DOWNS = "Castle Downs";
	private static final String CENTURY_PK = "Century Pk";
	private static final String YELLOWBIRD = "Yellowbird";
	private static final String SOUTH_CAMPUS = "South Campus";
	private static final String LEGER = "Leger";
	private static final String MEADOWS = "Mdws"; // "Meadows";
	private static final String HARRY_AINLAY = "Harry Ainlay";
	private static final String RUTHERFORD = "Rutherford";
	private static final String SOUTHWOOD = "Southwood";
	private static final String PARKALLEN = "Parkallen";
	private static final String KNOTTWOOD = "Knottwood";
	private static final String BELVEDERE = "Belvedere";
	private static final String BELEVEDERE = "Belevedere"; // TODO Belvedere?
	private static final String BONNIE_DOON = "Bonnie Doon";
	private static final String TAMARACK = "Tamarack";
	private static final String LESSARD = "Lessard";
	private static final String LYMBURN = "Lymburn";
	private static final String CARLTON = "Carlton";
	private static final String WEDGEWOOD = "Wedgewood";
	private static final String MISTATIM_IND = "Mistatim Ind";
	private static final String STADIUM = "Stadium";
	private static final String LAGO_LINDO = "Lago Lindo";
	private static final String MONTROSE = "Montrose";
	private static final String KINGSWAY = "Kingsway";
	private static final String KING_EDWARD_PK = "King Edward Pk";
	private static final String LONDONDERRY = "Londonderry";
	private static final String EVERGREEN = "Evergreen";
	private static final String FRASER = "Fraser";
	private static final String SPRUCE_GRV = "Spruce Grv";
	private static final String KLARVATTEN = "Klarvatten";
	private static final String RIVERDALE = "Riverdale";
	private static final String WINTERBURN = "Winterburn";
	private static final String WINTERBURN_IND = WINTERBURN + " Ind";
	private static final String STRATHCONA = "Strathcona";
	private static final String WINDSOR_PARK = "Windsor Pk";
	private static final String BELGRAVIA = "Belgravia";
	private static final String LAKEWOOD = "Lakewood";
	private static final String LAUDERDALE = "Lauderdale";
	private static final String CALDER = "Calder";
	private static final String HENWOOD = "Henwood";
	private static final String ST_ALBERT_TRAIL = "St Albert Trl";
	private static final String JASPER_GATE = "Jasper Gt";
	private static final String MAPLE_RIDGE = "Maple Rdg";
	private static final String SECORD = "Secord";
	private static final String S_ = "S ";
	private static final String ROSSDALE = "Rossdale";
	private static final String NORTH = "North";
	private static final String HIGHLANDS = "Highlands";
	private static final String LAUREL = "Laurel";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute.getRouteLongName());
	}

	private String cleanRouteLongName(String gRouteLongName) {
		gRouteLongName = CleanUtils.cleanStreetTypes(gRouteLongName);
		return CleanUtils.cleanLabel(gRouteLongName);
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		if (mRoute.simpleMergeLongName(mRouteToMerge)) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		if (isGoodEnoughAccepted()) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		System.out.printf("\nUnexpected routes to merge: %s & %s!\n", mRoute, mRouteToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return gRoute.getRouteShortName();
		}
		if (Utils.isDigitsOnly(gRoute.getRouteId())) {
			return gRoute.getRouteId();
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String AGENCY_COLOR_BLUE = "2D3092"; // BLUE (from Wikipedia SVG)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign2(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getHeadsignValue().startsWith(S_)) {
			if (!mTripToMerge.getHeadsignValue().startsWith(S_)) {
				mTrip.setHeadsignString(mTripToMerge.getHeadsignValue(), mTrip.getHeadsignId());
				return true;
			}
		} else if (mTripToMerge.getHeadsignValue().startsWith(S_)) {
			mTripToMerge.setHeadsignString(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignId());
			return true;
		}
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					CAPILANO //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CAPILANO, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CAPILANO, // <>
					WEST_EDM_MALL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					WEST_EDM_MALL, // <>
					LESSARD //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LESSARD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					WEST_EDM_MALL, // <>
					HIGHLANDS, //
					DOWNTOWN, //
					CLAREVIEW //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 3L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					CROMDALE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CROMDALE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 4L) {
			if (Arrays.asList( //
					BONNIE_DOON, //
					SOUTH_CAMPUS, //
					UNIVERSITY, // <>
					CAPILANO //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CAPILANO, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					WEST_EDM_MALL, //
					UNIVERSITY, // <>
					LEWIS_FARMS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LEWIS_FARMS, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					S_ + WEST_EDM_MALL, //
					S_ + UNIVERSITY, //
					S_ + LEWIS_FARMS //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(S_ + LEWIS_FARMS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 5L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					COLISEUM //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(COLISEUM, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 6L) {
			if (Arrays.asList( //
					MILLGATE, //
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 7L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					JASPER_PLACE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(JASPER_PLACE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					DOWNTOWN, //
					UNIVERSITY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 8L) {
			if (Arrays.asList( //
					KINGSWAY, // <>
					MILLGATE, // <>
					DOWNTOWN, // <>
					COLISEUM, //
					NORTH, //
					NAIT, //
					ABBOTTSFIELD //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ABBOTTSFIELD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					DOWNTOWN, // <>
					KINGSWAY, // <>
					MILLGATE, // <>
					LAKEWOOD, //
					BONNIE_DOON, //
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					KINGSWAY, //
					SOUTHGATE, //
					CENTURY_PK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					NORTHGATE, //
					NAIT, //
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 10L) {
			if (Arrays.asList( //
					BELVEDERE, // <>
					CLAREVIEW //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					BELVEDERE, // <>
					COLISEUM //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(COLISEUM, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					"88" + _ST + _AND_ + "132" + _AVE, //
					NORTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 12L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					KINGSWAY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(KINGSWAY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 14L) {
			if (Arrays.asList( //
					JASPER_PLACE, //
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId()); // Jasper Pl
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					DOWNTOWN, // <>
					NAIT, //
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					DOWNTOWN, // <>
					MILLGATE, //
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 16L) {
			if (Arrays.asList( //
					EAUX_CLAIRES, // <>
					CASTLE_DOWNS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CASTLE_DOWNS, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					EAUX_CLAIRES, // <>
					NORTHGATE, //
					GOV_CTR //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(GOV_CTR, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 23L) {
			if (Arrays.asList( //
					CENTURY_PK, // <>
					LEGER, //
					WEST_EDM_MALL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CENTURY_PK, // <>
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 30L) {
			if (Arrays.asList( //
					LEGER, //
					CENTURY_PK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (Arrays.asList( //
					SOUTHGATE, //
					MEADOWS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MEADOWS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (Arrays.asList( //
					SOUTHGATE, //
					MEADOWS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MEADOWS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 36L) {
			if (Arrays.asList( //
					LEGER, //
					CENTURY_PK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 39L) {
			if (Arrays.asList( //
					RUTHERFORD, // <>
					CENTURY_PK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (Arrays.asList( //
					YELLOWBIRD, //
					CENTURY_PK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 44L) {
			if (Arrays.asList( //
					CENTURY_PK, //
					SOUTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 46L) {
			if (Arrays.asList( //
					CENTURY_PK, // <>
					HARRY_AINLAY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(HARRY_AINLAY, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CENTURY_PK, // <>
					YELLOWBIRD //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(YELLOWBIRD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 51L) {
			if (Arrays.asList( //
					PARKALLEN, // <>
					UNIVERSITY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 52L) {
			if (Arrays.asList( //
					ROSSDALE, //
					STRATHCONA, //
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					DOWNTOWN, // <>
					GOV_CTR, //
					SOUTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 54L) {
			if (Arrays.asList( //
					SOUTH_CAMPUS, // <>
					UNIVERSITY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 55L) {
			if (Arrays.asList( //
					SOUTH_CAMPUS, // <>
					SOUTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					BELGRAVIA, //
					SOUTH_CAMPUS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(SOUTH_CAMPUS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 60L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					MILLGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 61L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					MILLGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 62L) {
			if (Arrays.asList( //
					MILL_WOODS_TC, //
					SOUTHWOOD //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(SOUTHWOOD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 63L) {
			if (Arrays.asList( //
					MILL_WOODS_TC, //
					SOUTHWOOD //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(SOUTHWOOD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 65L) {
			if (Arrays.asList( //
					KNOTTWOOD, // <>
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 66L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					MILLGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILLGATE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LAKEWOOD, //
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 67L) {
			if (Arrays.asList( //
					MEADOWS, //
					SILVERBERRY, //
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 68L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					MILLGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 69L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					MILLGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 70L) {
			if (Arrays.asList( //
					STRATHCONA, //
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 74L) {
			if (Arrays.asList( //
					CENTURY_PK, //
					SOUTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 81L) {
			if (Arrays.asList( //
					STRATHCONA, //
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					MILLGATE, //
					MILL_WOODS_TC //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILL_WOODS_TC, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 87L) {
			if (Arrays.asList( //
					S_ + STRATHCONA, //
					S_ + MILLGATE, //
					MILLGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 89L) {
			// TODO split 89
			if (Arrays.asList( //
					MEADOWS, //
					TAMARACK, //
					MEADOWS + _SLASH_ + TAMARACK // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MEADOWS + _SLASH_ + TAMARACK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 95L) {
			if (Arrays.asList( //
					LAUREL, //
					MEADOWS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MEADOWS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 99L) {
			if (Arrays.asList( //
					COLISEUM, // <>
					CAPILANO //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CAPILANO, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					COLISEUM, // <>
					BELVEDERE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BELVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 100L) {
			if (Arrays.asList( //
					WEST_EDM_MALL, //
					LEWIS_FARMS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LEWIS_FARMS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 102L) {
			if (Arrays.asList( //
					WEST_EDM_MALL, // <>
					LYMBURN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LYMBURN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 106L) {
			if (Arrays.asList( //
					WEST_EDM_MALL, // <>
					LESSARD // <>
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LESSARD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LESSARD, // <>
					WEST_EDM_MALL, // <>
					UNIVERSITY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 109L) {
			if (Arrays.asList( //
					JASPER_PLACE, //
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 111L) {
			if (Arrays.asList( //
					JASPER_PLACE, //
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 112L) {
			if (Arrays.asList( //
					DOWNTOWN, // <>
					WEST_EDM_MALL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					DOWNTOWN, // <>
					CAPILANO //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CAPILANO, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 117L) {
			if (Arrays.asList( //
					LEWIS_FARMS, //
					WEST_EDM_MALL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, mTrip.getHeadsignId()); // Lewis Farms
				return true;
			}
		} else if (mTrip.getRouteId() == 125L) {
			if (Arrays.asList( //
					S_ + WESTMOUNT, // <>
					S_ + JASPER_PLACE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(S_ + JASPER_PLACE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					WESTMOUNT, // <>
					JASPER_PLACE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(JASPER_PLACE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					JASPER_PLACE, // <>
					WESTMOUNT, // <>
					STADIUM, //
					COLISEUM, //
					KINGSWAY, //
					BELEVEDERE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BELEVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 130L) {
			if (Arrays.asList( //
					NAIT, //
					NORTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 137L) {
			if (Arrays.asList( //
					NORTHGATE, //
					CLAREVIEW //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 140L) {
			if (Arrays.asList( //
					NORTHGATE, // <>
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LAGO_LINDO, //
					NORTHGATE // <>
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 143L) {
			if (Arrays.asList( //
					COLISEUM, // <>
					KINGSWAY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(KINGSWAY, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					COLISEUM, // <>
					MONTROSE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MONTROSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 145L) {
			if (Arrays.asList( //
					"88" + _ST + _AND_ + "132" + _AVE, //
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					S_ + EAUX_CLAIRES, //
					S_ + LAGO_LINDO, //
					LAGO_LINDO //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LAGO_LINDO, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 150L) {
			if (Arrays.asList( //
					JASPER_PLACE, // <>
					WESTMOUNT, //
					NORTHGATE, //
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					JASPER_PLACE, // <>
					WEST_EDM_MALL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 151L) {
			if (Arrays.asList( //
					CALDER, // <>
					LAUDERDALE, // <>
					NAIT, //
					CASTLE_DOWNS //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CASTLE_DOWNS, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CALDER, // <>
					LAUDERDALE, // <>
					DOWNTOWN, //
					KING_EDWARD_PK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(KING_EDWARD_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 153L) {
			if (Arrays.asList( //
					S_ + CLAREVIEW, //
					S_ + BELVEDERE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(S_ + BELVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 161L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					GOV_CTR, //
					NORTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 162L) {
			if (Arrays.asList( //
					EAUX_CLAIRES, // <>
					CASTLE_DOWNS // <>
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CASTLE_DOWNS, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CASTLE_DOWNS, // <>
					DOWNTOWN, //
					GOV_CTR, //
					EAUX_CLAIRES // <>
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 168L) {
			if (Arrays.asList( //
					CARLTON, //
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 180L) {
			if (Arrays.asList( //
					BELVEDERE, // <>
					ABBOTTSFIELD //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ABBOTTSFIELD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					BELVEDERE, // <>
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 181L) {
			if (Arrays.asList( //
					LONDONDERRY, //
					BELVEDERE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BELVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 182L) {
			if (Arrays.asList( //
					CLAREVIEW, // <>
					NORTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CLAREVIEW, // <>
					FRASER //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(FRASER, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 186L) {
			if (Arrays.asList( //
					CLAREVIEW, //
					NORTHGATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 187L) {
			if (Arrays.asList( //
					CLAREVIEW, //
					LONDONDERRY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LONDONDERRY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 188L) {
			if (Arrays.asList( //
					LONDONDERRY, // <>
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LONDONDERRY, // <>
					CLAREVIEW //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 191L) {
			if (Arrays.asList( //
					KLARVATTEN, // <>
					EAUX_CLAIRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 302L) {
			if (Arrays.asList( //
					HENWOOD, // <>
					CLAREVIEW //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					HENWOOD, // <>
					EVERGREEN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(EVERGREEN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 303L) {
			if (Arrays.asList( //
					ST_ALBERT_TRAIL, //
					MISTATIM_IND //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MISTATIM_IND, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 305L) {
			if (Arrays.asList( //
					WESTMOUNT, // <>
					JASPER_GATE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(JASPER_GATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 306L) {
			if (Arrays.asList( //
					BONNIE_DOON, //
					MAPLE_RIDGE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MAPLE_RIDGE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 309L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					RIVERDALE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(RIVERDALE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 315L) {
			if (Arrays.asList( //
					SECORD, //
					WINTERBURN_IND //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WINTERBURN_IND, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 318L) {
			if (Arrays.asList( //
					UNIVERSITY, // <>
					BELGRAVIA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BELGRAVIA, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					UNIVERSITY, // <>
					WINDSOR_PARK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WINDSOR_PARK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 562L) {
			if (Arrays.asList( //
					WEST_EDM_MALL, // <>
					"WEM" + _AND_ + SOUTH_CAMPUS //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("WEM" + _AND_ + SOUTH_CAMPUS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 594L) {
			if (Arrays.asList( //
					LEWIS_FARMS, //
					WEST_EDM_MALL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge: %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(101L, new RouteTripSpec(101L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5660", // <> 187 Street & 52 Avenue #WedgeWood
								"5968", // <> Wedgewood Boulevard Loop
								"5888", // !=
								"5908", // 187 Street & 52 Avenue
								"5003", // West Edmonton Mall Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5687", // 163 Street & 91 Avenue #WEM
								"5003", // == West Edmonton Mall Transit Centre
								"5863", // !=
								"5721", // ==
								"5834", // ++
								"5673", // ==
								"5412", // !=
								"5891", // !=
								"5948", // ==
								"5856", // !=
								"5660", // <> 187 Street & 52 Avenue #WedgeWood
								"5968", // <> Wedgewood Boulevard Loop
						})) //
				.compileBothTripSort());
		map2.put(560L, new RouteTripSpec(560L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM, // DOWNTOWN
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8743", // Aspenglen Drive & Grove Drive #SpruceGrove
								"8584", // ++ Jennifer Heil Way & TransAlta Tri Leisure Centre
								"8761", // Century Road & Grove Drive
								"8910", // == Century Road & Vanderbilt Common
								"5415", // != 154 Street & 119 Avenue =>
								"1890", // == 109 Street & Princess Elizabeth Avenue
								"1050", // == 101 Street & 105 Avenue
								"1570", // != 101 Street & 103A Avenue =>
								"11334", // == 102 Street & 104 Avenue
								"1479", // != 97 Street & 103A Avenue =>
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1570", // != 101 Street & 103A Avenue #Edmonton <=
								"1860", // != 109 Street & Princess Elizabeth Avenue
								"5389", // != 154 Street & 118 Avenue <=
								"8371", // == Century Road & Kings Link
								"8743", // == Aspenglen Drive & Grove Drive
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	private static final Pattern N_A_I_T = Pattern.compile("((^|\\W){1}(n a i t)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String N_A_I_T_REPLACEMENT = "$2" + NAIT + "$4";

	private static final Pattern SUPER_EXPRESS = Pattern.compile("((^|\\W){1}(super express)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+( )?)", Pattern.CASE_INSENSITIVE);

	private static final String VIA = " via ";

	public String cleanTripHeadsign2(String tripHeadsign) {
		int indexOfVIA = tripHeadsign.toLowerCase(Locale.ENGLISH).indexOf(VIA);
		if (indexOfVIA >= 0) {
			tripHeadsign = tripHeadsign.substring(0, indexOfVIA); // remove "via ..."
		}
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TRANSIT_CENTER.matcher(tripHeadsign).replaceAll(TRANSIT_CENTER_REPLACEMENT);
		tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
		tripHeadsign = SUPER_EXPRESS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = INTERNATIONAL.matcher(tripHeadsign).replaceAll(INTERNATIONAL_REPLACEMENT);
		tripHeadsign = INDUSTRIAL_.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
		tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		// !ONLY USED FOR stop trip head sign!
		int indexOfVIA = tripHeadsign.toLowerCase(Locale.ENGLISH).indexOf(VIA);
		if (indexOfVIA >= 0) {
			tripHeadsign = tripHeadsign.substring(indexOfVIA); // keep "via ..."
		}
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TRANSIT_CENTER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
		tripHeadsign = SUPER_EXPRESS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = N_A_I_T.matcher(tripHeadsign).replaceAll(N_A_I_T_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern TRANSIT_CENTER = Pattern.compile("((^|\\W){1}(transit center|transit centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TRANSIT_CENTER_REPLACEMENT = "$2" + TRANSIT_CENTER_SHORT + "$4";

	private static final Pattern TOWN_CENTER = Pattern.compile("((^|\\W){1}(town center|town centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TOWN_CENTER_REPLACEMENT = "$2" + "TC" + "$4";

	private static final Pattern INTERNATIONAL = Pattern.compile("((^|\\W){1}(international)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INTERNATIONAL_REPLACEMENT = "$2" + "Int" + "$4";

	private static final Pattern INDUSTRIAL_ = Pattern.compile("((^|\\W){1}(industrial)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + "Ind" + "$4";

	private static final Pattern EDMONTON = Pattern.compile("((^|\\W){1}(edmonton)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = "$2" + EDM + "$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = TRANSIT_CENTER.matcher(gStopName).replaceAll(TRANSIT_CENTER_REPLACEMENT);
		gStopName = TOWN_CENTER.matcher(gStopName).replaceAll(TOWN_CENTER_REPLACEMENT);
		gStopName = INTERNATIONAL.matcher(gStopName).replaceAll(INTERNATIONAL_REPLACEMENT);
		gStopName = INDUSTRIAL_.matcher(gStopName).replaceAll(INDUSTRIAL_REPLACEMENT);
		gStopName = EDMONTON.matcher(gStopName).replaceAll(EDMONTON_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Math.abs(super.getStopId(gStop)); // remove negative stop IDs
	}

	private static final Pattern REMOVE_STARTING_DASH = Pattern.compile("(^\\-)", Pattern.CASE_INSENSITIVE);

	@Override
	public String getStopCode(GStop gStop) {
		String stopCode = super.getStopCode(gStop); // do not change, used by real-time API
		stopCode = REMOVE_STARTING_DASH.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		return stopCode; // do not change, used by real-time API
	}
}
