import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

/// Leave a screen that was opened from somewhere: back to where the user came from, or to Home when there is nowhere to go back to.
void leaveToWhereYouCameFrom(BuildContext context) {
  if (context.canPop()) {
    context.pop();
  } else {
    context.go('/home');
  }
}
