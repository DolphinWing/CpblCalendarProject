import 'package:flutter/material.dart';

import 'cpbl.dart';

class Query {
  int year;
  int month;
  FieldId field;
  GameType type;

  Query({this.year, this.month, this.field = FieldId.f00, this.type = GameType.type_01});
}

class DrawerPane extends StatefulWidget {
  final ValueChanged<Query> onPressed;
  final int year;
  final int month;
  final GameType type;

  DrawerPane({this.year = 2018, this.month = 10, this.type = GameType.type_01, this.onPressed});

  @override
  State<StatefulWidget> createState() => _DrawerPaneState();
}

class _DrawerPaneState extends State<DrawerPane> {
  int _year;
  int _month;
  FieldId _field;
  GameType _type;

  @override
  void initState() {
    super.initState();
    //var now = DateTime.now();
    setState(() {
      _year = widget.year;
      _month = widget.month;
      _field = FieldId.f00;
      _type = widget.type;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Drawer(
      //constraints: BoxConstraints(minWidth: 240),
      child: Padding(
        child: Column(
          children: <Widget>[
            Expanded(
              child: ListView(
                children: <Widget>[
                  Text('year'),
                  _YearMenuWidget(
                    year: widget.year,
                    onValueChanged: (value) {
                      setState(() {
                        _year = value;
                      });
                    },
                  ),
                  Text('month'),
                  _MonthMenuWidget(
                    month: widget.month,
                    onValueChanged: (value) {
                      setState(() {
                        _month = value;
                      });
                    },
                  ),
                  Text('field'),
                  _FieldMenuWidget(
                    id: FieldId.f00,
                    onValueChanged: (value) {
                      setState(() {
                        _field = value;
                      });
                    },
                  ),
                  Text('type'),
                  _TypeMenuWidget(
                    type: widget.type,
                    onValueChanged: (value) {
                      setState(() {
                        _type = value;
                      });
                    },
                  ),
                  Text('teams'),
                  _TeamMenuWidget(),
                ],
              ),
            ),
            //Expanded(child: SizedBox()),
            Container(
              constraints: BoxConstraints.expand(height: 48),
              child: RaisedButton(
                child: Text('query'),
                onPressed: () {
                  Navigator.of(context).pop();
                  if (widget.onPressed != null) {
                    widget.onPressed(new Query(
                      year: _year,
                      month: _month,
                      field: _field,
                      type: _type,
                    ));
                  }
                },
                color: Theme.of(context).primaryColor,
              ),
              margin: EdgeInsets.only(bottom: 8, right: 16),
            ),
          ],
        ),
        padding: EdgeInsets.only(top: 16, bottom: 16, left: 32, right: 16),
      ),
      //color: Colors.white,
    );
  }
}

class _YearMenuWidget extends StatefulWidget {
  final int year;
  final ValueChanged<int> onValueChanged;

  _YearMenuWidget({this.year = 1990, this.onValueChanged});

  @override
  State<StatefulWidget> createState() => _YearMenuWidgetState();
}

class _YearMenuWidgetState extends State<_YearMenuWidget> {
  int _year;

  @override
  void initState() {
    super.initState();
    setState(() {
      _year = widget.year;
    });
  }

  @override
  Widget build(BuildContext context) {
    var now = DateTime.now();
    List<DropdownMenuItem<int>> list = new List();
    for (int i = now.year; i >= 1990; i--) {
      list.add(new DropdownMenuItem(
        child: Text('Year ${i - 1989} ($i)'),
        value: i,
      ));
    }
    return DropdownButton<int>(
      value: _year,
      items: list,
      onChanged: (value) {
        setState(() {
          _year = value;
        });
        if (widget.onValueChanged != null) {
          widget.onValueChanged(value);
        }
      },
      isExpanded: true,
    );
  }
}

class _MonthMenuWidget extends StatefulWidget {
  final int month;
  final ValueChanged<int> onValueChanged;

  _MonthMenuWidget({this.month = DateTime.january, this.onValueChanged});

  @override
  State<StatefulWidget> createState() => _MonthMenuWidgetState();
}

class _MonthMenuWidgetState extends State<_MonthMenuWidget> {
  static const List<int> MONTHS = [
    DateTime.january,
    DateTime.february,
    DateTime.march,
    DateTime.april,
    DateTime.may,
    DateTime.june,
    DateTime.july,
    DateTime.august,
    DateTime.september,
    DateTime.october,
    DateTime.november,
    DateTime.december,
  ];
  int _month;

  @override
  void initState() {
    super.initState();
    setState(() {
      _month = widget.month;
    });
  }

  @override
  Widget build(BuildContext context) {
    List<DropdownMenuItem<int>> list = new List();
    MONTHS.forEach((m) {
      list.add(DropdownMenuItem(
        value: m,
        child: Text('month $m'),
      ));
    });
    return DropdownButton<int>(
      value: _month,
      items: list,
      onChanged: (value) {
        setState(() {
          _month = value;
        });
        if (widget.onValueChanged != null) {
          widget.onValueChanged(value);
        }
      },
      isExpanded: true,
    );
  }
}

class _FieldMenuWidget extends StatefulWidget {
  final FieldId id;
  final ValueChanged<FieldId> onValueChanged;

  _FieldMenuWidget({this.id = FieldId.f00, this.onValueChanged});

  @override
  State<StatefulWidget> createState() => _FieldMenuWidgetState();
}

class _FieldMenuWidgetState extends State<_FieldMenuWidget> {
  FieldId _id;

  @override
  void initState() {
    super.initState();
    setState(() {
      _id = widget.id;
    });
  }

  @override
  Widget build(BuildContext context) {
    List<DropdownMenuItem<FieldId>> list = new List();
    FieldId.values.forEach((id) {
      list.add(DropdownMenuItem(
        value: id,
        child: Text(CpblClient.getFieldName(context, id)),
      ));
    });
    return DropdownButton<FieldId>(
      value: _id,
      onChanged: (value) {
        setState(() {
          _id = value;
        });
        if (widget.onValueChanged != null) {
          widget.onValueChanged(value);
        }
      },
      items: list,
      isExpanded: true,
    );
  }
}

class _TypeMenuWidget extends StatefulWidget {
  final ValueChanged<GameType> onValueChanged;
  final GameType type;

  _TypeMenuWidget({this.type = GameType.type_01, this.onValueChanged});

  @override
  State<StatefulWidget> createState() => _TypeMenuWidgetState();
}

class _TypeMenuWidgetState extends State<_TypeMenuWidget> {
  static const List<GameType> TYPES = [
    GameType.type_01,
    GameType.type_03,
    GameType.type_05,
    GameType.type_07,
  ];

  GameType _type;

  @override
  void initState() {
    super.initState();
    setState(() {
      _type = widget.type;
    });
  }

  @override
  Widget build(BuildContext context) {
    List<DropdownMenuItem<GameType>> list = new List();
    TYPES.forEach((type) {
      list.add(DropdownMenuItem(
        value: type,
        child: Text(CpblClient.getGameType(context, type)),
      ));
    });
    return DropdownButton<GameType>(
      value: _type,
      items: list,
      onChanged: (value) {
        setState(() {
          _type = value;
        });
        if (widget.onValueChanged != null) {
          widget.onValueChanged(value);
        }
      },
      isExpanded: true,
    );
  }
}

class _TeamMenuWidget extends StatefulWidget {
  final bool enabled;
  final ValueChanged<TeamId> onValueChanged;

  _TeamMenuWidget({this.enabled = true, this.onValueChanged});

  @override
  State<StatefulWidget> createState() => _TeamMenuWidgetState();
}

class _TeamMenuWidgetState extends State<_TeamMenuWidget> {
  TeamId _id;

  @override
  void initState() {
    super.initState();
    setState(() {
      _id = TeamId.unknown;
    });
  }

  @override
  Widget build(BuildContext context) {
    return widget.enabled
        ? DropdownButton<TeamId>(
            value: _id,
            items: [
              DropdownMenuItem(
                value: TeamId.unknown,
                child: Text('all teams'),
              ),
            ],
            onChanged: (value) {
              setState(() {
                _id = value;
              });
              if (widget.onValueChanged != null) {
                widget.onValueChanged(value);
              }
            },
            isExpanded: true,
          )
        : SizedBox(height: 1);
  }
}
