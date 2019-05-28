import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_widget_from_html_core/flutter_widget_from_html_core.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:launch_review/launch_review.dart';

import 'cpbl.dart';
import 'lang.dart';

class UiMode {
  static const int list = 0;
  static const int quick = 1;
  static const int pager = 2;
}

class ContentUiWidget extends StatefulWidget {
  final bool enabled;
  final bool loading;
  final List<Game> list;
  final int year;
  final int month;
  final int mode;
  final ValueChanged<bool> onScrollEnd;
  final GestureTapCallback onHighlightPaneClosed;
  final TeamId favTeamId;
  final FieldId fieldId;

  ContentUiWidget({
    this.enabled = true,
    this.loading = false,
    List<Game> list,
    this.year,
    this.month,
    this.mode = UiMode.list,
    this.onScrollEnd,
    this.onHighlightPaneClosed,
    TeamId favTeam,
    FieldId field,
  })  : this.list = list ?? new List(),
        this.favTeamId = favTeam ?? TeamId.fav_all,
        this.fieldId = field ?? FieldId.f00;

  @override
  State<StatefulWidget> createState() => _ContentUiWidgetState();
}

class _ContentUiWidgetState extends State<ContentUiWidget> {
  ScrollController _controller;
  bool _end = false;

  @override
  void initState() {
    super.initState();
    _controller = new ScrollController();
    _controller.addListener(() {
      //print('atEdge = ${_controller.position.atEdge} ${_controller.position.pixels}');
      if (_controller.position.atEdge && _controller.position.pixels > 0) {
        setState(() {
          _end = true;
        });
        if (widget.onScrollEnd != null) widget.onScrollEnd(true);
      }
      if (_end && !_controller.position.atEdge) {
        setState(() {
          _end = false;
        });
        if (widget.onScrollEnd != null) widget.onScrollEnd(false);
      }
    });
  }

  Widget _buildContent(BuildContext context, List<Game> list, int mode, bool enabled) {
    //print('target ${widget.fieldId} ${widget.favTeamId}');
    List<Widget> widgetList = new List();
    list?.forEach((game) {
      switch (game.type) {
        case GameType.announcement:
          widgetList.add(_GameCardAnnouncementWidget(message: game.extra));
          break;
        case GameType.update:
          widgetList.add(_AppUpdateCardWidget(game.extra));
          break;
        case GameType.more:
          widgetList.add(_GameCardMoreWidget(
            onPressed: widget.onHighlightPaneClosed,
          ));
          break;
        default:
          if (widget.favTeamId == TeamId.fav_all && widget.fieldId == FieldId.f00) {
            widgetList.add(GameCardWidget(game, enabled: enabled, mode: mode));
          } else if (widget.favTeamId == TeamId.fav_all) {
            if (widget.fieldId == game.fieldId) {
              widgetList.add(GameCardWidget(game, enabled: enabled, mode: mode));
            }
          } else if (widget.fieldId == FieldId.f00) {
            if (game.isFav(widget.favTeamId)) {
              widgetList.add(GameCardWidget(game, enabled: enabled, mode: mode));
            }
          } else {
            if (widget.fieldId == game.fieldId && game.isFav(widget.favTeamId)) {
              widgetList.add(GameCardWidget(game, enabled: enabled, mode: mode));
            }
          }
          break;
      }
    });
    print('show list ${widgetList.length}');
//    var now = DateTime.now();
//    if (now.year == widget.year && now.month == widget.month && widget.mode != UiMode.quick) {
//      var i = 0;
//      widgetList.forEach((f) {
//        if (f is GameCardWidget) {
//          if (f.game.before()) {
//            i++;
//          }
//        }
//      });
//      print('try auto scroll... $i');
//      new Timer(const Duration(seconds: 1), () {
//        _controller.jumpTo(160 * i.toDouble());
//      });
//    }
    return ListView(
      controller: _controller,
      children: widgetList.isNotEmpty
          ? widgetList
          : [
              SizedBox(height: 64),
              (widget.year != null && widget.month != null)
                  ? Container(
                      constraints: BoxConstraints.expand(height: 64),
//                      child: Text(
//                        '${widget.year}/${widget.month}',
//                        textAlign: TextAlign.center,
//                        style: Theme.of(context).textTheme.subhead,
//                      ),
                    )
                  : SizedBox(
                      height: 64,
                    ),
              Container(
                constraints: BoxConstraints.expand(height: 64),
                child: Text(
                  Lang.of(context).trans(enabled ? 'no_data' : 'loading_data'),
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.title,
                ),
              ),
            ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: Alignment.center,
      children: <Widget>[
        _buildContent(context, widget.list, widget.mode, widget.enabled && !widget.loading),
        widget.loading ? Positioned(child: CircularProgressIndicator(), top: 256) : SizedBox(),
      ],
    );
  }
}

class TeamRowWidget extends StatelessWidget {
  final Team team;

  TeamRowWidget(this.team);

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        Icon(
          Icons.donut_small,
          color: team.color,
        ),
        SizedBox(width: 16),
        Expanded(
          child: Text(
            team.getDisplayName(context),
            style: Theme.of(context).textTheme.title,
          ),
        ),
        Padding(
          child: Text(
            '${team.score}',
            style: Theme.of(context).textTheme.title,
          ),
          padding: EdgeInsets.all(4),
        ),
      ],
    );
  }
}

class GameCardBaseWidget extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  final GestureTapCallback onPressed;

  //final GestureLongPressCallback onLongPressed;
  final Color backgroundColor;

  GameCardBaseWidget({
    @required this.child,
    bool usePadding = true,
    EdgeInsets padding,
    this.onPressed,
    //this.onLongPressed,
    Color backgroundColor,
  })  : this.backgroundColor = backgroundColor ?? Colors.transparent,
        this.padding = padding ?? EdgeInsets.all(usePadding ? 16 : 0);

  @override
  Widget build(BuildContext context) {
    return new Container(
      padding: padding,
      margin: EdgeInsets.only(top: 4, bottom: 4, left: 8, right: 8),
      child: this.onPressed != null
          ? FlatButton(
              child: child,
              onPressed: this.onPressed,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(8))),
              highlightColor: Theme.of(context).backgroundColor,
            )
          : Padding(
              padding: EdgeInsets.fromLTRB(16, 0, 16, 0),
              child: child,
            ),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(8)),
        //color: Colors.grey.withAlpha(32),
        border: Border.all(
          style: BorderStyle.solid,
          color: Colors.grey.withAlpha(64),
        ),
        color: backgroundColor,
        //color: Colors.grey.withAlpha(48),
      ),
    );
  }
}

class GameCardWidget extends StatelessWidget {
  final Game game;
  final int mode;
  final bool enabled;

  GameCardWidget(this.game, {int mode = UiMode.list, this.enabled = true})
      : this.mode = mode ?? UiMode.list;

  void showCpblUrl(BuildContext context, String url) async {
    //print('launch $url');
    //if (await canLaunch(url)) {
    await CpblClient.launchUrl(context, url);
    //} else {
    //  throw 'Could not launch $url';
    //}
  }

  @override
  Widget build(BuildContext context) {
    return new GameCardBaseWidget(
      child: Padding(
        padding: EdgeInsets.fromLTRB(8, 16, 8, 16),
        child: new Column(
          children: <Widget>[
            //Divider(),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: <Widget>[
                Text(
                  game.getDisplayTime(),
                  style: Theme.of(context).textTheme.subhead,
                ),
                Expanded(child: SizedBox()),
                Text(
                  '${game.getGameType(context)} ${game.id}',
                  style: Theme.of(context).textTheme.subtitle,
                ),
              ],
            ),
            TeamRowWidget(game.away),
            TeamRowWidget(game.home),
            SizedBox(
              height: 4,
            ),
            _GameCardExtraWidget(
              fieldName: game.getFieldName(context),
              extraMessage: game.extra,
              channel: game.channel,
            ),
            //Divider(),
          ],
        ),
      ),
      usePadding: false,
      onPressed: /*this.mode != UiMode.quick &&*/ game.url != null
          ? () {
              if (enabled) showCpblUrl(context, game.url);
            }
          : null,
    );
  }
}

class _GameCardExtraWidget extends StatefulWidget {
  final String fieldName;
  final String extraMessage;
  final String channel;

  _GameCardExtraWidget({this.fieldName, this.extraMessage, this.channel});

  @override
  State<StatefulWidget> createState() => _GameCardExtraWidgetState();
}

class _GameCardExtraWidgetState extends State<_GameCardExtraWidget> {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Text(widget.fieldName),
        (widget.channel != null)
            ? Padding(
                child: GestureDetector(
                  child: Icon(
                    Icons.live_tv,
                    color: Colors.red,
                    size: 24,
                  ),
                  onTap: () {
                    Fluttertoast.showToast(
                      msg: widget.channel ?? "no channel",
                      toastLength: Toast.LENGTH_SHORT,
                      backgroundColor: Colors.green,
                      textColor: Colors.white,
                    );
                  },
                ),
                padding: EdgeInsets.only(left: 8, right: 8),
              )
            : SizedBox(),
        Expanded(
          child: widget.extraMessage != null
              ? Text('${widget.extraMessage}', textAlign: TextAlign.end)
              : SizedBox(height: 1),
        ),
      ],
    );
  }
}

class _GameCardAnnouncementWidget extends StatelessWidget {
  final String message;

  _GameCardAnnouncementWidget({String message}) : this.message = message ?? '';

  @override
  Widget build(BuildContext context) {
    return GameCardBaseWidget(
      child: Container(
        child: Center(
          child: Text(message),
        ),
        constraints: BoxConstraints(minHeight: 64),
      ),
    );
  }
}

class _GameCardMoreWidget extends StatelessWidget {
  final GestureTapCallback onPressed;

  _GameCardMoreWidget({this.onPressed});

  @override
  Widget build(BuildContext context) {
    return GameCardBaseWidget(
      child: Container(
        child: Text(
          Lang.of(context).trans('drawer_more'),
          style: TextStyle(color: Theme.of(context).primaryColor),
        ),
        padding: EdgeInsets.fromLTRB(8, 16, 8, 16),
      ),
      usePadding: false,
      onPressed: this.onPressed,
      //backgroundColor: Theme.of(context).primaryColor.withAlpha(16),
    );
  }
}

class _AppUpdateCardWidget extends StatelessWidget {
  final String message;

  _AppUpdateCardWidget(String message) : this.message = message ?? '';

  @override
  Widget build(BuildContext context) {
    return GameCardBaseWidget(
      padding: EdgeInsets.fromLTRB(8, 16, 8, 0),
      child: Column(
        children: <Widget>[
          Container(
            child: Padding(
              child: Center(child: HtmlWidget(message)),
              padding: EdgeInsets.all(0),
            ),
            constraints: BoxConstraints(minHeight: 64),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[
              FlatButton(
                textColor: Theme.of(context).primaryColor,
                child: Text(Lang.of(context).trans('drawer_update_now')),
                onPressed: () {
                  /// https://stackoverflow.com/a/50782200/2673859
                  LaunchReview.launch();
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class PagerSelectorWidget extends StatefulWidget {
  final bool enabled;
  final int year;
  final ValueChanged<int> onYearChanged;
  final ValueChanged<FieldId> onFieldChanged;
  final ValueChanged<TeamId> onFavTeamChanged;
  final List<FieldId> fieldList;
  final List<TeamId> teamList;

  PagerSelectorWidget({
    this.enabled = true,
    int year = 2018,
    this.onYearChanged,
    this.onFieldChanged,
    this.onFavTeamChanged,
    List<FieldId> fieldList,
    List<TeamId> teamList,
  })  : this.year = year ?? DateTime.now().year,
        this.fieldList = fieldList ?? FieldId.values,
        this.teamList = teamList ?? CpblClient.favTeams;

  @override
  State<StatefulWidget> createState() => _PagerSelectorWidgetState();
}

class _PagerSelectorWidgetState extends State<PagerSelectorWidget> {
  int _year;
  FieldId _fieldId;
  TeamId _favTeam;

  @override
  void initState() {
    super.initState();
    setState(() {
      _year = widget.year;
      _fieldId = FieldId.f00;
      _favTeam = TeamId.fav_all;
    });
  }

  _onYearChipPressed() async {
    if (!widget.enabled) return;
    //print('show year selector');
    final r = await showDialog(
        context: context,
        builder: (context) {
          int yearNow = DateTime.now().year;
          List<Widget> options = new List();
          for (int year = yearNow; year >= 1990; year--) {
            options.add(ChipMenuOption(
              year,
              Lang.of(context)
                      .trans('drawer_entry_year')
                      .replaceAll('@year', (year - 1989).toString()) +
                  ' ($year)',
            ));
          }
          return ChipMenuDialog('drawer_title_year', options);
        });
    print('result = $r');
    if (r != null) {
      setState(() {
        _year = r;
      });
      if (widget.onYearChanged != null) widget.onYearChanged(r);
    }
  }

  _onFieldChipPressed() async {
    if (!widget.enabled) return;
    //print('show field selector');
    final r = await showDialog(
        context: context,
        builder: (context) {
          List<Widget> options = new List();
          widget.fieldList?.forEach((id) {
            options.add(ChipMenuOption(id, CpblClient.getFieldName(context, id)));
          });
          return ChipMenuDialog('drawer_title_field', options);
        });
    print('result = $r');
    if (r != null) {
      setState(() {
        _fieldId = r;
      });
      if (widget.onFieldChanged != null) widget.onFieldChanged(r);
    }
  }

  _onTeamChipPressed() async {
    if (!widget.enabled) return;
    //print('show team selector');
    final r = await showDialog(
        context: context,
        builder: (context) {
          List<Widget> options = new List();
          widget.teamList?.forEach((id) {
            options.add(ChipMenuOption(id, CpblClient.getTeamName(context, id)));
          });
          return ChipMenuDialog('drawer_title_team', options);
        });
    print('result = $r');
    if (r != null) {
      setState(() {
        _favTeam = r;
      });
      if (widget.onFavTeamChanged != null) widget.onFavTeamChanged(r);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Row(
        children: <Widget>[
          SizedBox(width: 8),
          ActionChip(
            label: Text(
              Lang.of(context)
                  .trans('drawer_entry_year')
                  .replaceAll('@year', (_year - 1989).toString()),
            ),
            onPressed: _onYearChipPressed,
            pressElevation: 2.0,
          ),
          SizedBox(width: 8),
          ActionChip(
            label: Text(CpblClient.getFieldName(context, _fieldId)),
            pressElevation: 2.0,
            onPressed: _onFieldChipPressed,
          ),
          SizedBox(width: 8),
          ActionChip(
            label: Text(CpblClient.getTeamName(context, _favTeam)),
            pressElevation: 2.0,
            onPressed: _onTeamChipPressed,
          ),
        ],
      ),
      decoration: BoxDecoration(
        borderRadius: BorderRadiusDirectional.only(
          topEnd: Radius.circular(16),
          topStart: Radius.circular(16),
        ),
        color: Colors.white,
      ),
      //padding: EdgeInsets.only(top: 8, bottom: 8),
    );
  }
}

class ChipMenuDialog extends StatelessWidget {
  final String titleResKey;
  final List<Widget> options;

  ChipMenuDialog(String title, this.options) : this.titleResKey = title ?? "";

  @override
  Widget build(BuildContext context) {
    return SimpleDialog(
      //title: Text(Lang.of(context).trans(titleResKey)),
      //titlePadding: EdgeInsets.fromLTRB(24, 16, 24, 0),
      children: options,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(8))),
    );
  }
}

class ChipMenuOption extends StatelessWidget {
  final dynamic value;
  final String text;

  ChipMenuOption(this.value, this.text);

  @override
  Widget build(BuildContext context) {
    return SimpleDialogOption(
      onPressed: () {
        Navigator.pop(context, this.value);
      },
      child: Text(this.text, style: Theme.of(context).textTheme.subhead),
    );
  }
}
