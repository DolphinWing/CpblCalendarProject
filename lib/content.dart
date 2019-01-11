import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

import 'cpbl.dart';
import 'lang.dart';

class UiMode {
  static const int list = 0;
  static const int quick = 1;
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

  Widget buildContent(BuildContext context, List<Game> list, int mode, bool enabled) {
    //print('target ${widget.fieldId} ${widget.favTeamId}');
    List<Widget> widgetList = new List();
    list?.forEach((game) {
      switch (game.type) {
        case GameType.announcement:
          widgetList.add(_GameCardAnnouncementWidget(message: game.extra));
          break;
        case GameType.update:
          widgetList.add(Text(game.extra));
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
    return ListView(
      controller: _controller,
      children: widgetList.isNotEmpty
          ? widgetList
          : [
              SizedBox(height: 64),
              (widget.year != null && widget.month != null)
                  ? Container(
                      constraints: BoxConstraints.expand(height: 64),
                      child: Text(
                        '${widget.year}/${widget.month}',
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.subhead,
                      ),
                    )
                  : SizedBox(
                      height: 64,
                    ),
              Container(
                constraints: BoxConstraints.expand(height: 64),
                child: Text(
                  Lang.of(context).trans('no_data'),
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.title,
                ),
              ),
            ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return widget.loading
        ? Stack(
            alignment: Alignment.center,
            children: <Widget>[
              buildContent(context, widget.list, widget.mode, widget.enabled && !widget.loading),
              Positioned(child: CircularProgressIndicator(), top: 256),
            ],
          )
        : buildContent(context, widget.list, widget.mode, widget.enabled && !widget.loading);
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
  final bool padding;
  final GestureTapCallback onPressed;

  GameCardBaseWidget({@required this.child, this.padding = true, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return new Container(
      padding: EdgeInsets.all(this.padding ? 16 : 0),
      margin: EdgeInsets.only(top: 4, bottom: 4, left: 8, right: 8),
      child: this.onPressed != null
          ? FlatButton(
              child: child,
              onPressed: this.onPressed,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(8))),
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
          color: Colors.grey.withAlpha(48),
        ),
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
            Row(
              children: <Widget>[
                Text(game.getFieldName(context)),
                Expanded(
                  child: game.extra != null
                      ? Text('${game.extra}', textAlign: TextAlign.end)
                      : SizedBox(height: 1),
                ),
              ],
            ),
            //Divider(),
          ],
        ),
      ),
      padding: false,
      onPressed: this.mode == UiMode.list && game.url != null
          ? () {
              if (enabled) showCpblUrl(context, game.url);
            }
          : null,
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
      child: Padding(
        child: Text(Lang.of(context).trans('drawer_more')),
        padding: EdgeInsets.fromLTRB(8, 16, 8, 16),
      ),
      padding: false,
      onPressed: this.onPressed,
    );
  }
}

class PagerSelectorWidget extends StatefulWidget {
  final bool enabled;
  final int year;
  final ValueChanged<int> onYearChanged;
  final ValueChanged<FieldId> onFieldChanged;
  final ValueChanged<TeamId> onFavTeamChanged;

  PagerSelectorWidget({
    this.enabled = true,
    int year = 2018,
    this.onYearChanged,
    this.onFieldChanged,
    this.onFavTeamChanged,
  }) : this.year = year ?? DateTime.now().year;

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
        //FIXME: reset fav teams
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
          FieldId.values.forEach((id) {
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
          CpblClient.favTeams.forEach((id) {
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

class SettingsGroupTitle extends StatelessWidget {
  final String titleResKey;

  SettingsGroupTitle(String titleResKey) : this.titleResKey = titleResKey ?? '';

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Text(
        Lang.of(context).trans(titleResKey),
        style: Theme.of(context).textTheme.caption,
      ),
      constraints: BoxConstraints.expand(height: 36),
      padding: EdgeInsets.fromLTRB(16, 0, 8, 8),
      //color: Colors.grey.withAlpha(32),
      alignment: Alignment.bottomLeft,
    );
  }
}
