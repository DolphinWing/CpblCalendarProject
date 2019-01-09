import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

import 'cpbl.dart';

class UiMode {
  static const int list = 0;
  static const int quick = 1;
}

class ContentUiWidget extends StatefulWidget {
  final bool loading;
  final List<Game> list;
  final int mode;
  final ValueChanged<bool> onScrollEnd;
  final GestureTapCallback onHighlightPaneClosed;

  ContentUiWidget({
    this.loading = false,
    List<Game> list,
    this.mode = UiMode.list,
    this.onScrollEnd,
    this.onHighlightPaneClosed,
  }) : this.list = list ?? new List();

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

  Widget buildContent(BuildContext context, List<Game> list, int mode) {
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
          widgetList.add(GameCardWidget(game));
          break;
      }
    });
    return ListView(
      controller: _controller,
      children: widgetList,
    );
  }

  @override
  Widget build(BuildContext context) {
    return widget.loading
        ? Stack(
            alignment: Alignment.center,
            children: <Widget>[
              buildContent(context, widget.list, widget.mode),
              Positioned(child: CircularProgressIndicator(), top: 200),
            ],
          )
        : buildContent(context, widget.list, widget.mode);
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
        Padding(
          child: Icon(
            Icons.donut_small,
            color: team.color,
          ),
          padding: EdgeInsets.all(8),
        ),
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

  GameCardBaseWidget({@required this.child});

  @override
  Widget build(BuildContext context) {
    return new Container(
      padding: EdgeInsets.only(left: 16, right: 16, top: 8, bottom: 8),
      margin: EdgeInsets.only(top: 4, bottom: 4, left: 8, right: 8),
      child: child,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(8)),
        //color: Colors.grey.withAlpha(32),
        border: Border.all(
          style: BorderStyle.solid,
          color: Colors.grey.withAlpha(48),
        ),
      ),
    );
  }
}

class GameCardWidget extends StatelessWidget {
  final Game game;

  GameCardWidget(this.game);

  @override
  Widget build(BuildContext context) {
    return new GameCardBaseWidget(
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
    return Padding(
      child: RaisedButton(
        child: Text('more'),
        onPressed: onPressed,
      ),
      padding: EdgeInsets.only(left: 16, right: 16),
    );
  }
}
