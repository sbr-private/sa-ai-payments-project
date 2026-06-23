export function requireAuth(req, res, next) {
  if (!req.session?.user) {
    req.session.returnTo = req.originalUrl;
    return res.redirect('/login');
  }
  next();
}

export function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.session?.user) {
      return res.redirect('/login');
    }
    if (!roles.includes(req.session.user.role)) {
      const portal = req.session.user.role === 'support' ? '/support' : '/payer';
      return res.status(403).render('error', {
        title: 'Wrong portal',
        message: `Your account (${req.session.user.email}) does not have access to this area.`,
        hint: `Try the ${req.session.user.role === 'support' ? 'Control Centre' : 'Payer Portal'} instead.`,
        actionHref: portal,
        actionLabel: 'Go to your portal',
      });
    }
    next();
  };
}

export function attachUser(req, res, next) {
  res.locals.user = req.session?.user ?? null;
  res.locals.currentPath = req.path;
  next();
}
