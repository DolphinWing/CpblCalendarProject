import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'lang.dart';

enum TeamId {
  elephants,
  ct_elephants,
  lions,
  lions_711,
  eda_rhinos,
  lamigo_monkeys,
  sinon_bulls,
  lanew_bears,
  first_kinkon,
  jungo_bears,
  eagles,
  tigers,
  dragons,
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

  final TeamId id;
  String name;
  int score;
  final Color color;
  final bool _home;

  bool isHomeTeam() => _home;

  String getDisplayName(BuildContext context) =>
      name ?? Lang.of(context).trans('cpbl_team_name_${id.toString().substring(7)}');
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
  f16,
  f17,
  f19,
  f23,
  f26,
}

enum GameType {
  type_01,
  type_03,
  type_05,
  type_07,
  type_09,
  type_16,
}

class Game {
  Game({
    this.id = 0,
    this.type = GameType.type_01,
    Team home,
    Team away,
    this.fieldId = FieldId.f00,
    DateTime time,
    this.isDelayed = false,
    bool isFinal,
    this.extra,
  })  : this._time = time ?? DateTime.now(),
        this.home = home ?? Team(homeTeam: true),
        this.away = away ?? Team(homeTeam: false),
        this.isFinal = isFinal ?? (time?.isBefore(DateTime.now()) ?? false);

  final int id;
  final GameType type;

  final Team home;
  final Team away;

  final FieldId fieldId;
  final DateTime _time;

  bool isDelayed;
  bool isFinal;

  String extra;

  String _padLeft(int num) => num.toString().padLeft(2, '0');

  String getDisplayTime() => '${_time.year}/${_padLeft(_time.month)}/${_padLeft(_time.day)} '
      '${_padLeft(_time.hour)}:${_padLeft(_time.minute)}';

  String getFieldName(BuildContext context) =>
      Lang.of(context).trans('cpbl_game_field_name_${fieldId.toString().substring(8)}');

  String getGameType(BuildContext context) =>
      Lang.of(context).trans('cpbl_game_${type.toString().substring(9)}');
}

class CpblClient {
  static const _url = 'http://www.cpbl.com.tw/schedule';

}
