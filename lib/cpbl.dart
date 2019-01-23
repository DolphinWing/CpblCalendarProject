import 'package:firebase_remote_config/firebase_remote_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter_custom_tabs/flutter_custom_tabs.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

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

  static Map<String, Team> idMap = new Map()
    ..putIfAbsent('E02', () => ctBrothers)
    ..putIfAbsent('L01', () => lions711)
    ..putIfAbsent('A02', () => lamigoMonkeys)
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
        color: Colors.redAccent,
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

  static Team parse(String png, [int year, bool homeTeam = false]) {
    Team team = Team.simple(homeTeam ? TeamId.unknown_home : TeamId.unknown_away, homeTeam);
    String key = png.substring(0, 3);
    if (idMap.containsKey(key)) {
      switch (idMap[key].id) {
        case TeamId.first_kinkon: //2003
        case TeamId.lanew_bears: //2010
        case TeamId.lamigo_monkeys:
          if (year <= 2003) {
            team = firstKinkon;
          } else if (year <= 2010) {
            team = laNewBears;
          } else {
            team = lamigoMonkeys;
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

  String getDisplayName(BuildContext context) => name ?? CpblClient.getTeamName(context, id);
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
    this.isLive = false,
    this.extra,
  })  : this._time = time ?? DateTime.now(),
        this.home = home ?? Team(homeTeam: true),
        this.away = away ?? Team(homeTeam: false),
        this.isFinal = isFinal ?? (time?.isBefore(DateTime.now()) ?? false);

  Game.update(String message) : this(id: -2, type: GameType.update, extra: message);

  Game.more() : this(id: -1, type: GameType.more);

  Game.announce(int key, String message)
      : this(id: key, type: GameType.announcement, extra: message);

  Game.simple(int id, {GameType type = GameType.type_01, TeamId homeId, TeamId awayId})
      : this(id: id, type: type, home: Team.simple(homeId, true), away: Team.simple(awayId, false));

  final int id;
  final GameType type;
  DateTime _time;

  Team home;
  Team away;

  FieldId fieldId;

  bool isDelayed;
  bool isFinal;
  bool isLive;

  String channel;
  String extra;
  String url;

  String _padLeft(int num) => num.toString().padLeft(2, '0');

  String _displayDate() => '${_time.year}/${_padLeft(_time.month)}/${_padLeft(_time.day)}';

  String _displayTime() => '${_padLeft(_time.hour)}:${_padLeft(_time.minute)}';

  String getDisplayTime() => isFinal ? _displayDate() : '${_displayDate()} ${_displayTime()}';

  String getFieldName(BuildContext context) => CpblClient.getFieldName(context, fieldId);

  String getGameType(BuildContext context) => CpblClient.getGameType(context, type);

  void changeTime(int hour, int minute) {
    _time = new DateTime(_time.year, _time.month, _time.day, hour, minute);
  }

  //http://www.cpbl.com.tw/games/box.html?&game_type=01&game_id=198&game_date=2015-10-01&pbyear=2015
  String getUrl() => url ?? '';

  void refreshStatus() {
    var now = DateTime.now();
    if (isLive && (now.millisecondsSinceEpoch - _time.millisecondsSinceEpoch) > 86400000) {
      isLive = false;
      isDelayed = true;
    }
  }

  bool before([DateTime time]) => _time.isBefore(time ?? DateTime.now());

  bool after([DateTime time]) => _time.isAfter(time ?? DateTime.now());

  bool isToday([DateTime time]) {
    DateTime t = time ?? DateTime.now();
    return t.year == _time.year && t.month == _time.month && t.day == _time.day;
  }

  int timeInMillis() => _time.millisecondsSinceEpoch;

  bool isFav(TeamId id) => home.id == id || away.id == id;
}

class CpblClient {
  static const homeUrl = 'http://www.cpbl.com.tw';
  static const _scheduleUrl = '$homeUrl/schedule';

  /// https://stackoverflow.com/a/49709147/2673859
  static bool get debug {
    bool debugMode = false;
    assert(debugMode = true);
    return debugMode;
  }

  String _padLeft(int num) => num.toString().padLeft(2, '0');

  static String getFieldName(BuildContext context, FieldId id) =>
      Lang.of(context).trans('cpbl_game_field_name_${id.toString().substring(8)}');

  static String getGameType(BuildContext context, GameType type) =>
      Lang.of(context).trans('cpbl_game_${type.toString().substring(9)}');

  static String getTeamName(BuildContext context, TeamId id) =>
      Lang.of(context).trans('cpbl_team_name_${id.toString().substring(7)}');

  Map<String, FieldId> fieldMap = new Map();

  FieldId parseField(String field) {
    //print('field = $field');
    return fieldMap.containsKey(field) ? fieldMap[field] : FieldId.f00;
  }

  static const String allstar_month_override = '4/6;6/11;8/6;10/8;11/8;13/6;';
  static const String season_month_start_override = '8/2;9/2;15/2;19/3;';
  static const String warmup_month_start_override = '19/2;';
  static const String challenge_month_override =
      '1998/10/11;1999/10;2005/10;2006/10;2007/10;2008/10;2017/10;2018/10';
  static const String champ_month_override =
      '1991/11;1992/0;1994/0;1995/0;1996/10/11;1998/11;1999/11;2004/11;2005/10/11;2008/10/11;2017/10/11;2018/10/11';

  final Map<int, int> warmUpMonthOverrides = new Map();
  final Map<int, int> allStarMonthOverrides = new Map();
  final Map<int, int> seasonStartMonthOverrides = new Map();
  final Map<int, String> challengeGames = new Map();
  final Map<int, String> championMonthOverrides = new Map();

  bool isWarmUpMonth(int year, int month) {
    if (year < 2006) return false;
    if (warmUpMonthOverrides.containsKey(year)) return warmUpMonthOverrides[year] == month;
    return month == DateTime.march;
  }

  int getAllStarMonth(int year) => allStarMonthOverrides[year] ?? DateTime.july;

  bool isAllStarMonth(int year, int month) => getAllStarMonth(year) == month;

  bool isChallengeMonth(int year, int month) {
    if (challengeGames.containsKey(year)) {
      String monthStr = challengeGames[year];
      if (monthStr.isEmpty) {
        return false;
      } else if (monthStr.contains('/')) {
        monthStr.split('/').forEach((m) {
          if (int.parse(m) == month) {
            return true;
          }
        });
      } else {
        return int.parse(monthStr) == month;
      }
    }
    return false;
  }

  bool isChampionMonth(int year, int month) {
    if (championMonthOverrides.containsKey(year)) {
      String monthStr = championMonthOverrides[year];
      if (monthStr.isEmpty) {
        return false;
      } else if (monthStr.contains('/')) {
        monthStr.split('/').forEach((m) {
          if (int.parse(m) == month) {
            return true;
          }
        });
      } else {
        return int.parse(monthStr) == month;
      }
    } else {
      return month == DateTime.october;
    }
    return false;
  }

  bool hasGames(int year, int month) {
    switch (month) {
      case DateTime.january:
      case DateTime.december:
        return false;

      //check season start
      case DateTime.february:
      case DateTime.march:
        if (year < 2006) {
          if (seasonStartMonthOverrides.containsKey(year)) {
            return seasonStartMonthOverrides[year] <= month;
          } else {
            return DateTime.march <= month;
          }
        } else {
          //we have warm up games
          return isWarmUpMonth(year, month);
        }
        break;

      case DateTime.november:
        return isChampionMonth(year, month);

      default:
        return true;
    }
  }

  static const List<TeamId> favTeams = [
    TeamId.fav_all,
    TeamId.ct_brothers,
    TeamId.lions_711,
    TeamId.fubon_guardians,
    TeamId.lamigo_monkeys
  ];

  static const List<String> monthList = [
    'jan',
    'feb',
    'mar',
    'apr',
    'may',
    'jun',
    'jul',
    'aug',
    'sep',
    'oct',
    'nov',
    'dec'
  ];

  static String getMonthString(BuildContext context, int month) {
    if (month == null || month < DateTime.january || month > DateTime.december) {
      return 'unkonwn';
    }
    return Lang.of(context).trans('drawer_entry_month_${CpblClient.monthList[month - 1]}');
  }

//  HttpClient _client;
  var _client = new http.Client();
  final RemoteConfig _configs;
  final SharedPreferences _prefs;
  final int currentVersionCode;

  CpblClient(BuildContext context, RemoteConfig configs, SharedPreferences prefs,
      {int versionCode = 0})
      : this._configs = configs,
        this._prefs = prefs,
        this.currentVersionCode = versionCode ?? 0 {
//    _client = new HttpClient();
//    _client.connectionTimeout = new Duration(seconds: 10);
    print('constructor');
    //prepare field map
    FieldId.values.forEach((id) {
      //print('id: $id, ${getFieldName(context, id)}');
      fieldMap.putIfAbsent(getFieldName(context, id), () => id);
    });
    fieldMap[Lang.of(context).trans('cpbl_game_field_name_f19r')] = FieldId.f19;
    fieldMap[Lang.of(context).trans('cpbl_game_field_name_f23r')] = FieldId.f23;
    fieldMap[Lang.of(context).trans('cpbl_game_field_name_f26r')] = FieldId.f26;
  }

  Future<int> init() async {
    print('init cpbl client');
    _cachedGameList.clear(); //clear cache

    //load warm up overrides
    var warm = warmup_month_start_override.split(';');
    warm.forEach((value) {
      if (value.isNotEmpty && value.contains('/')) {
        var ym = value.split('/');
        warmUpMonthOverrides.putIfAbsent(int.parse(ym[0]) + 1989, () => int.parse(ym[1]));
      }
    });
    //load all star override
    var allStar = allstar_month_override.split(';');
    allStar.forEach((value) {
      if (value.isNotEmpty && value.contains('/')) {
        var ym = value.split('/');
        allStarMonthOverrides.putIfAbsent(int.parse(ym[0]) + 1989, () => int.parse(ym[1]));
      }
    });
    //load season start override
    var season = season_month_start_override.split(';');
    season.forEach((value) {
      if (value.isNotEmpty && value.contains('/')) {
        var ym = value.split('/');
        seasonStartMonthOverrides.putIfAbsent(int.parse(ym[0]) + 1989, () => int.parse(ym[1]));
      }
    });
    //load challenge games
    var challenge = challenge_month_override.split(';');
    challenge.forEach((value) {
      if (value.isNotEmpty && value.contains('/')) {
        var ym = value.split('/');
        challengeGames.putIfAbsent(int.parse(ym[0]), () => ym[1]);
      }
    });
    //load champion games override
    var champion = challenge_month_override.split(';');
    champion.forEach((value) {
      if (value.isNotEmpty && value.contains('/')) {
        var ym = value.split('/');
        championMonthOverrides.putIfAbsent(int.parse(ym[0]), () => ym[1]);
      }
    });

    //prepare first read
    //await _client.read(homeUrl)
    /*await*/
    _client.get(homeUrl);
//    .then((html) {
//      print('html = ${html.length}');
//    });
    return 0;
  }

  final Map<String, List<Game>> _cachedGameList = new Map();
  final _cacheDelay = const Duration(milliseconds: 200);
  final _finalDelay = const Duration(milliseconds: 400);

  Future<List<Game>> fetchList(int year, int month,
      [GameType type = GameType.type_01, bool cached = true]) async {
    print('fetch $year/$month type = $type');
    String url = '$_scheduleUrl/index/$year-$month-01.html?'
        '&date=$year-$month-01&gameno=01&sfieldsub=&sgameno=${type.toString().substring(14)}';
    //print(url);

    String key = '$year/$month-$type';
    if (cached && _cachedGameList.containsKey(key)) {
      return Future.delayed(_cacheDelay, () => _cachedGameList[key]);
    }

//    var request = await _client.getUrl(Uri.parse(url));
//    var resp = await request.close();
//    await for(var content in resp.transform(utf8.decoder)) {
//      print('${content.length} ${content.contains('one_block')}');
//    }

    final response = await _client.get(url);
    //print('response.body ${response?.body?.length} ${response?.body?.indexOf('one_block')}');

//    final response = await http.get(url);
//    //.timeout(const Duration(seconds: 5), onTimeout: () => '');
    List<Game> list = new List();
    String html = response?.body ?? '';
    //print('response.body ${html?.length} ${html.indexOf('one_block')}');
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
      //print('blocks: ${oneBlock.length}');
      for (int i = 1; i < oneBlock.length; i++) {
        //print('$i) ${oneBlock[i]}');
        var block = oneBlock[i];
        if (block.contains('<!-- one_block -->')) {
          block = block.substring(0, block.lastIndexOf('<!-- one_block -->'));
        }
        Game g = _parseGameFromBlock(block, year, month, type);
        if (g == null) {
          //print('no game in this block ($i)');
          continue;
        }
        _extractTeamFromBlock(g, block, year);
        _extractScheduleFromBlock(g, block);
        _extractPlayInfoFromBlock(g, block);
        //print('url: ${g.url}');
        list.add(g);
      }
    } else {
      print('no data');
    }
    print('game list size:${list.length}');
    _cachedGameList[key] = list;
    //return list;
    return Future.delayed(_finalDelay, () => _cachedGameList[key]);
  }

  Game _parseGameFromBlock(String block, int year, int month, GameType type) {
    RegExp expId = new RegExp('game_id=([0-9]+)');
    var id = expId.firstMatch(block)?.group(1);
    if (id == null) {
      //print('bypass this block');
      return null;
    }
    //print('game id = $id');
    RegExp expDate = new RegExp('game_date=([\\d]+)-([\\d]+)-([\\d]+)');
    var date = expDate.firstMatch(block);
    int day = -1;
    if (date != null) {
      //print('>>> date: ${date.group(1)} ${date.group(2)} ${date.group(3)}');
      day = int.parse(date.group(3));
    } else {
      print('no day');
      return null;
    }
    return new Game(id: int.parse(id), time: new DateTime(year, month, day), type: type);
  }

  Game _extractTeamFromBlock(Game g, String block, int year) {
    if (block.contains('class="schedule_team')) {
      String matchUpPlace = block.substring(block.indexOf('class="schedule_team'));
      matchUpPlace = matchUpPlace.substring(0, matchUpPlace.indexOf("</table"));
      var tds = matchUpPlace.split('<td');
      if (tds.length > 3) {
        String awayTeam = tds[1];
        awayTeam = awayTeam.substring(awayTeam.indexOf('images/team/'));
        awayTeam = awayTeam.substring(12, awayTeam.indexOf(".png"));
        g.away = Team.parse(awayTeam, year);
        String place = tds[2];
        place = place.substring(place.indexOf('>') + 1, place.indexOf('</td>')).trim();
        g.fieldId = parseField(place);
        String homeTeam = tds[3];
        homeTeam = homeTeam.substring(homeTeam.indexOf('images/team/'));
        homeTeam = homeTeam.substring(12, homeTeam.indexOf(".png"));
        g.home = Team.parse(homeTeam, year, true);
        //print('${g.id} $awayTeam vs $homeTeam @$place');
      } else {
        print('no match up');
      }
    }
    return g;
  }

  Game _extractScheduleFromBlock(Game g, String block) {
    if (block.contains('schedule_info')) {
      var info = block.split('schedule_info');
      //schedule_info[1] contains game id and if this is delayed game or not
      if (info.length > 1) {
        var extras = info[1].split('<th');
        if (info[1].contains('class="sp"')) {
          if (extras.length > 1) {
            //check delay game
            String title = extras[1];
            //print('title = $title');
            title = title.substring(title.indexOf('>') + 1, title.indexOf('</th'));
            title = title.replaceAll('\r', '').replaceAll('\n', '').trim();
            g.extra = title;
          }
          //print('>>> delayed game');
          g.isDelayed = true;
        }
        if (extras.length > 3) {
          //more info to find
          String data = extras[3];
          data = data.substring(data.indexOf('>') + 1, data.indexOf('</th')).trim();
          if (data?.isNotEmpty == true) {
            g.extra = (g.extra != null) ? '${g.extra} $data' : data;
          }
        }
      }
      //schedule_info[2] contains results
      if (info.length > 2 && info[2].contains('schedule_score')) {
        RegExp expScore = new RegExp('schedule_score[^>]*>([\\d]+)');
        Iterable<Match> scores = expScore.allMatches(info[2]);
        if (scores.length >= 2) {
          var s1 = scores.elementAt(0).group(1);
          g.away.score = int.parse(s1);
          var s2 = scores.elementAt(1).group(1);
          g.home.score = int.parse(s2);
          //print('  score $s1 vs $s2');
        }
      }
      //schedule_info[3] contains delay messages
      if (info.length > 3 && info[3].contains('</td>')) {
        String message = info[3].trim();
        var msg = message.split('</td>');
        //check game time
        if (msg.length > 1) {
          String time = msg[1];
          try {
            time = time.substring(time.indexOf('>') + 1);
            if (time.contains(':') && (time.indexOf(':') == 2 || time.indexOf(':') == 1)) {
              var t = time.split(':');
              g.changeTime(int.parse(t[0]), int.parse(t[1]));
            }
          } catch (e) {
            e.printStackTrace();
          }
        }

        //check channel
        if (msg.length > 2) {
          String channel = msg[2];
          if (channel.contains('title=')) {
            channel = channel.substring(channel.indexOf('title=') + 7);
            channel = channel.substring(0, channel.indexOf('"'));
            g.channel = channel;
          }
        }

        //check delayed messages
        if (g.isDelayed && message.contains('schedule_sp_txt')) {
          message = message.substring(message.indexOf('schedule_sp_txt'));
          message = message.substring(message.indexOf('>') + 1, message.indexOf('<'));
        } else {
          message = null;
        }
        if (message?.isNotEmpty == true) {
          String delayMsg = message.replaceAll('<[^>]*>', '').replaceAll('[ ]+', ' ').trim();
          g.extra = g.extra != null ? '${g.extra} $delayMsg' : delayMsg;
        }
      }
    }
    return g;
  }

  Game _extractPlayInfoFromBlock(Game g, String block) {
    if (block?.contains('game_playing') == true) {
      int splitIndex = block.indexOf("game_playing");
      g?.isFinal = false;
      g?.isLive = true;
      String msg = block.substring(splitIndex);
      msg = msg.substring(msg.indexOf('>') + 1);
      msg = msg.substring(0, msg.indexOf('<'));
      g.extra = 'LIVE! $msg'; //replace all other messages

      String url;
      try {
        url = block.substring(0, splitIndex - 9);
        url = url.substring(url.lastIndexOf('href=') + 6);
      } catch (e) {
        e.printStackTrace();
      }
      g.url = url != null ? '$_scheduleUrl$url' : g.url;

      //dolphin++@20180821: keep games may not update correctly in CPBL website
      g.refreshStatus();
    } else if (block?.contains('schedule_icon_starter.png') == true) {
      //<a href="/games/starters.html?&game_type=01&game_id=9&game_date=2016-03-27&pbyear=2016">
      //<img src="http://cpbl-elta.cdn.hinet.net/web/images/schedule_icon_starter.png" width="20" height="18" /></a>
      String url;
      try {
        url = block.substring(0, block.indexOf('schedule_icon_starter.png'));
        url = url.substring(url.lastIndexOf('href=') + 6);
        url = url.substring(0, url.indexOf(">") - 1);
        //Log.d(TAG, "url = " + url);
      } catch (e) {
        e.printStackTrace();
      }
      g.url = url != null ? '$_scheduleUrl$url' : g.url;
    } else if (block?.contains("onClick") == true) {
      g.isFinal = true; //no playing
      g.url = '$homeUrl/games/box.html?'
          'game_date=${g._time.year}-${_padLeft(g._time.month)}-${_padLeft(g._time.day)}'
          '&game_id=${g.id}&pbyear=${g._time.year}&game_type=${g.type.toString().substring(14)}';
    }
    return g;
  }

  bool isHighlightEnabled() => _prefs.getBool('show_highlight') ?? true;

  void setHighlightEnabled(bool enabled) async {
    await _prefs.setBool('show_highlight', enabled);
  }

  bool isViewPagerEnabled() => _prefs.getBool('use_view_pager') ?? true;

  void setViewPagerEnabled(bool enabled) async {
    await _prefs.setBool('use_view_pager', enabled);
  }

  bool canUpdate() {
    int code = _configs.getInt('latest_version_code') ?? 0;
    print('app version = $currentVersionCode vs. store = $code');
    if (code > currentVersionCode) {
      print('have new version $code ${_configs.getString('latest_version_summary')}');
      return true;
    }
    return false;
  }

  String getUpdateSummary() => canUpdate() ? _configs.getString('latest_version_summary') : '';

  Future<List<Game>> loadRemoteAnnouncement() async {
    List<Game> announceList = new List();
    List<String> cards = _configs.getString('add_highlight_card').split(';');
    var now = DateTime.now();
    cards.forEach((key) {
      var t = DateTime(int.parse(key.substring(0, 4)), int.parse(key.substring(4, 6)),
          int.parse(key.substring(6)));
      print('$key ${t.year}/${t.month}/${t.day}');
      if (t.isAfter(now)) {
        announceList
            .add(Game.announce(int.parse(key), _configs.getString('add_highlight_card_$key')));
      }
    });
    return Future.delayed(_finalDelay, () => announceList);
  }

  List<Game> getHighlightGameList(List<Game> srcList, [DateTime time]) {
    print('check ${srcList.length} for $time');
    srcList.sort((g1, g2) => g1.timeInMillis() == g2.timeInMillis()
        ? g1.id - g2.id
        : g1.timeInMillis() - g2.timeInMillis());
    List<Game> dstList = new List();
    int beforeIndex = -1, afterIndex = -1;
    int beforeDiff = -1, afterDiff = -1;
    int now = (time ?? DateTime.now()).millisecondsSinceEpoch;
    for (int i = 0; i < srcList.length; i++) {
      Game g = srcList[i];
      //print('[${g.id}]@${g.getDisplayTime()}');
      int diff = g.timeInMillis() - now;
      if (g.before(time)) {
        //print('before $diff $beforeDiff $beforeIndex');
        if (g.isToday(time) && beforeDiff != 0) {
          beforeIndex = i;
          beforeDiff = 0;
        } else if (-diff < beforeDiff || beforeDiff < 0) {
          beforeDiff = -diff;
          beforeIndex = i;
        }
      } else if (g.after(time)) {
        //print('after $diff $afterDiff $afterIndex');
        if (g.isToday(time)) {
          afterIndex = i;
          afterDiff = diff;
        } else if (diff < afterDiff || afterDiff < 0) {
          //no games today, try to find the closest games
          afterDiff = diff;
          afterIndex = i;
        }
      }
    }
    print('beforeIndex = $beforeIndex, afterIndex = $afterIndex');
    bool lived = false;
    for (int i = beforeIndex >= 0 ? beforeIndex : 0; i < srcList.length; i++) {
      Game g = srcList[i];
      if (g.before(time)) {
        //final or live
        dstList.add(g); //add all of them
        lived |= g.isLive; //only no live games that we will show upcoming games
      } else if (!lived) {
        //don't show upcoming when games are live
        int diff = g.timeInMillis() - now;
        //Log.d(TAG, String.format("after: diff=%d", diff));
        if (diff > afterDiff) {
          //ignore all except the closest upcoming games
          //Log.d(TAG, "ignore all except the closest upcoming games");
          break;
        }
        dstList.add(g);
      }
    }
    if (lived && beforeIndex > 0) {
      //try to add closest result only
      int diff = now - srcList[beforeIndex].timeInMillis();
      for (int i = beforeIndex; i >= 0; i--) {
        Game g = srcList[i];
        if (g.isLive) continue; //don't add live games that we have already added
        int d = now - srcList[i].timeInMillis();
        if (diff < d) break; //ignore all other final result, just closest ones
        if (!dstList.contains(g)) {
          dstList.add(g);
        }
        diff = d; //store the diff
      }
    }
    return dstList;
  }

  bool isOverrideStartEnabled() => _configs.getBool('override_start_enabled');

  int getStartYear() =>
      isOverrideStartEnabled() ? _configs.getInt('override_start_year') : DateTime.now().year;

  int getStartMonth() =>
      isOverrideStartEnabled() ? _configs.getInt('override_start_month') : DateTime.now().month;

  bool isLoadGamesInHighlight() => !_configs.getBool('enable_highlight_no_games') ?? true;

  static Future<void> launchUrl(BuildContext context, String url) => launch(url,
      option: new CustomTabsOption(
        toolbarColor: Theme.of(context).primaryColor,
        enableUrlBarHiding: true,
        showPageTitle: true,
        animation: new CustomTabsAnimation.slideIn(),
        extraCustomTabs: <String>[
          // ref. https://play.google.com/store/apps/details?id=org.mozilla.firefox
          'org.mozilla.firefox',
          // ref. https://play.google.com/store/apps/details?id=com.microsoft.emmx
          'com.microsoft.emmx',
        ],
      ));
}
