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

  Future<List<Game>> fetchList(int year, int month) async {
    final response = await http.get(
      '$_url/index/$year-$month-01.html?&date=$year-$month-01&gameno=01&sfieldsub=',
    );
    List<Game> list = new List();
    if (response?.statusCode == 200) {
      String html = response.body;
      print('response.body ${html.length}');
      if (html?.contains('<div class="one_block"') == true) {
        html = html.substring(0, html.indexOf('<div class="footer">'));
//        var tdDays = html.split('<td valign="top">');
//        RegExp exp = new RegExp('<th class="[^"]*">([0-9]+)</th>');
//        Iterable<Match> matches = exp.allMatches(html);
//        int days = 0;
//        matches.forEach((m) {
//          days++;
//          if (tdDays[days].contains("one_block")) {
//            print('$days ${m.group(1)}');
//          }
//        });
        var oneBlock = html.split('<div class="one_block"');
        print('blocks: ${oneBlock.length}');
        for (int i = 1; i < oneBlock.length; i++) {
          //print('$i) ${oneBlock[i]}');
          var block = oneBlock[i];
          if (block.contains("<!-- one_block -->")) {
            block = block.substring(0, block.lastIndexOf("<!-- one_block -->"));
          }
          RegExp expId = new RegExp('game_id=([0-9]+)');
          var id = expId.firstMatch(block).group(1);
          print('game id = $id');
          RegExp expDate = new RegExp('game_date=([\\d]+)-([\\d]+)-([\\d]+)');
          var date = expDate.firstMatch(block);
          if (date != null) {
            print('>>> date: ${date.group(1)} ${date.group(2)} ${date.group(3)}');
          }
          if (block.contains("schedule_info")) {
            var info = block.split("schedule_info");
            //schedule_info[1] contains game id and if this is delayed game or not
            if (info.length > 1) {
              var extras = info[1].split("<th");
              if (info[1].contains('class="sp"')) {
                if (extras.length > 1) {//check delay game
                  String extraTitle = extras[1];
                  extraTitle = extraTitle.substring(extraTitle.indexOf(">") + 1,
                      extraTitle.indexOf("</th"));
                  extraTitle = extraTitle.replaceAll('\r', '').replaceAll('\n', '').trim();
                }
                print('>>> delayed game');
              }
              if (extras.length > 3) { //more info to find
                String data = extras[3];
                data = data.substring(data.indexOf(">") + 1, data.indexOf("</th"));
              }
            }
            //schedule_info[2] contains results
            if (info.length > 2 && info[2].contains("schedule_score")) {
              RegExp expScore = new RegExp('schedule_score[^>]*>([\\d]+)');
              Iterable<Match> scores = expScore.allMatches(info[2]);
              if (scores.length >= 2) {
                var s1 = scores.elementAt(0).group(1);
                var s2 = scores.elementAt(1).group(1);
                print('  score $s1 vs $s2');
              }
            }
            //schedule_info[3] contains delay messages
          }
          if (block.contains('class="schedule_team')) {
            String matchUpPlace = block.substring(block.indexOf('class="schedule_team'));
            matchUpPlace = matchUpPlace.substring(0, matchUpPlace.indexOf("</table"));
            var tds = matchUpPlace.split('<td');
            if (tds.length > 3) {
              String awayTeam = tds[1];
              awayTeam = awayTeam.substring(awayTeam.indexOf("images/team/"));
              awayTeam = awayTeam.substring(12, awayTeam.indexOf(".png"));
              String place = tds[2];
              place = place.substring(place.indexOf(">") + 1, place.indexOf("</td>"));
              String homeTeam = tds[3];
              homeTeam = homeTeam.substring(homeTeam.indexOf("images/team/"));
              homeTeam = homeTeam.substring(12, homeTeam.indexOf(".png"));
              print('  $awayTeam vs $homeTeam @$place');
            } else {
              print('no match up');
            }
          }
        }
      } else {
        print('no data');
      }
    } else {
      print('status code: ${response?.statusCode}');
    }
    return list;
  }
}
