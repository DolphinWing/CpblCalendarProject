
import 'package:flutter/material.dart';

import 'lang.dart';

enum TeamId {
  fav_all,
  elephants,
  ct_brothers,
  lions,
  lions_711,
  eda_rhinos,
  fubon_guardians,
  lamigo_monkeys,
  rakuten_monkeys,
  sinon_bulls,
  lanew_bears,
  first_kinkon,
  jungo_bears,
  times_eagles,
  ss_tigers,
  w_dragons,
  makoto_cobras,
  makoto_sun,
  media_t_rex,
  ct_whales,
  kg_whales,
  all_star_red,
  all_star_white,
  unknown_away,
  unknown_home,
  unknown
}

class Team {
  Team({TeamId id, this.name, this.score = 0, this.color = Colors.black, bool homeTeam = false})
      : this._home = homeTeam,
        this.id = id ?? (homeTeam ? TeamId.unknown_home : TeamId.unknown_away);

  Team.fromTeam(Team team, bool homeTeam)
      : this.id = team.id,
        this.name = team.name,
        this.color = team.color,
        this.score = team.score,
        this._home = homeTeam;

  Team.simple(TeamId id, bool homeTeam) : this(id: id, homeTeam: homeTeam);

  static Team get elephants => Team(
    id: TeamId.elephants,
    color: Colors.yellow,
  );

  static Team get lions => Team(
    id: TeamId.lions,
    color: Colors.green,
  );

  static Team get lions711 => Team(
    id: TeamId.lions_711,
    color: Colors.orange,
  );

  static Team get lamigoMonkeys => Team(
    id: TeamId.lamigo_monkeys,
    color: Colors.blueGrey,
  );

  static Team get rakutenMonkeys => Team(
    id: TeamId.rakuten_monkeys,
    color: Color.fromARGB(255, 127, 0, 32),
  );

  static Team get fubonGuardians => Team(
    id: TeamId.fubon_guardians,
    color: Colors.blueAccent,
  );

  static Team get edaRhinos => Team(
    id: TeamId.eda_rhinos,
    color: Colors.purple,
  );

  static Team get ctBrothers => Team(
    id: TeamId.ct_brothers,
    color: Colors.yellow,
  );

  static Team get sinonBulls => Team(
    id: TeamId.sinon_bulls,
    color: Colors.lightGreen,
  );

  static Team get ctWhales => Team(
    id: TeamId.ct_whales,
    color: Colors.blueAccent,
  );

  static Team get kgWhales => Team(
    id: TeamId.kg_whales,
    color: Colors.greenAccent,
  );

  static Team get mediaTRex => Team(
    id: TeamId.media_t_rex,
    color: Colors.deepOrange,
  );

  static Team get makotoCobras => Team(
    id: TeamId.makoto_cobras,
    color: Colors.deepOrange,
  );

  static Team get makotoSuns => Team(
    id: TeamId.makoto_sun,
    color: Colors.deepOrange,
  );

  static Team get firstKinkon => Team(
    id: TeamId.first_kinkon,
    color: Colors.blueGrey,
  );

  static Team get laNewBears => Team(
    id: TeamId.lanew_bears,
    color: Colors.blueGrey,
  );

  static Team get ssTigers => Team(
    id: TeamId.ss_tigers,
    color: Colors.blue,
  );

  static Team get wDragons => Team(
    id: TeamId.w_dragons,
    color: Colors.red,
  );

  static Team get timesEagles => Team(
    id: TeamId.times_eagles,
    color: Colors.black87,
  );

  static Team get jungoBears => Team(
    id: TeamId.jungo_bears,
    color: Colors.greenAccent,
  );

  static Team get allStarRed => Team(
    id: TeamId.all_star_red,
    color: Colors.red,
  );

  static Team get allStarWhite => Team(
    id: TeamId.all_star_white,
    color: Color.fromARGB(255, 244, 244, 244),
  );

  static Map<String, Team> idMap = new Map()
    ..putIfAbsent('E02', () => ctBrothers)
    ..putIfAbsent('L01', () => lions711)
    ..putIfAbsent('A02', () => lamigoMonkeys)
    ..putIfAbsent('A03', () => rakutenMonkeys)
    ..putIfAbsent('B04', () => fubonGuardians)
    ..putIfAbsent('B03', () => edaRhinos)
    ..putIfAbsent('E01', () => elephants)
    ..putIfAbsent('B02', () => sinonBulls)
    ..putIfAbsent('W01', () => ctWhales)
    ..putIfAbsent('G02', () => mediaTRex)
    ..putIfAbsent('G01', () => makotoCobras)
    ..putIfAbsent('A01', () => firstKinkon)
    ..putIfAbsent('T01', () => ssTigers)
    ..putIfAbsent('D01', () => wDragons)
    ..putIfAbsent('C01', () => timesEagles)
    ..putIfAbsent('B01', () => jungoBears)
    ..putIfAbsent('S01', () => allStarRed)
    ..putIfAbsent('S02', () => allStarWhite)
    ..putIfAbsent('S03', () => allStarRed)
    ..putIfAbsent('S04', () => allStarWhite)
    ..putIfAbsent('S05', () => allStarRed)
    ..putIfAbsent('S06', () => allStarWhite);

  static Team parse(String png, [int year, bool homeTeam = false]) {
    Team team = Team.simple(homeTeam ? TeamId.unknown_home : TeamId.unknown_away, homeTeam);
    String key = png.substring(0, 3);
    if (idMap.containsKey(key)) {
      switch (idMap[key].id) {
        case TeamId.first_kinkon: //2003
        case TeamId.lanew_bears: //2010
        case TeamId.lamigo_monkeys://2019
        case TeamId.rakuten_monkeys:
          if (year <= 2003) {
            team = firstKinkon;
          } else if (year <= 2010) {
            team = laNewBears;
          } else if (year <= 2019) {
            team = lamigoMonkeys;
          } else {
            team = rakutenMonkeys;
          }
          break;
        case TeamId.lions: //2006
        case TeamId.lions_711:
          team = year < 2007 ? lions : lions711;
          break;
        case TeamId.kg_whales: //2001
        case TeamId.ct_whales:
          team = year <= 2001 ? kgWhales : ctWhales;
          break;
        case TeamId.makoto_sun: //2003
        case TeamId.makoto_cobras:
          team = year <= 2003 ? makotoSuns : makotoCobras;
          break;
        default:
          team = idMap[key];
          break;
      }
    }
    return new Team.fromTeam(team, homeTeam);
  }

  final TeamId id;
  String name;
  int score;
  final Color color;
  final bool _home;

  bool isHomeTeam() => _home;

  String getDisplayName(BuildContext context) => name ?? CpblUtils.getTeamName(context, id);
}

enum FieldId {
  f00,
  f01,
  f02,
  f03,
  f04,
  f05,
  f06,
  f07,
  f08,
  f09,
  f10,
  f11,
  f12,
  f13,
  f14,
  f15,
  f17,
  f19,
  f23,
  f26,
}

enum GameType {
  type_01, //regular season
  type_02, //all-star game
  type_03, //championship
  type_05, //challenge games
  type_07, //warm up games
  type_09,
  type_16,
  update,
  announcement,
  more,
}

class CpblUtils {
  static String getFieldName(BuildContext context, FieldId id) =>
      Lang.of(context).trans('cpbl_game_field_name_${id.toString().substring(8)}');

  static String getGameType(BuildContext context, GameType type) =>
      Lang.of(context).trans('cpbl_game_${type.toString().substring(9)}');

  static String getTeamName(BuildContext context, TeamId id) =>
      Lang.of(context).trans('cpbl_team_name_${id.toString().substring(7)}');
}
