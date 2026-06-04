export const environment = {
  production: true,
  apiBase: '/change-app-service',
  authUrl:     'http://localhost:8888/ms-internal-iam/auth/authorize',
  tokenUrl:    'http://localhost:8888/ms-internal-iam/auth/token',
  logoutUrl:   'http://localhost:8888/ms-internal-iam/auth/logout',
  clientId:    'change-mgmt-web',
  redirectUri: 'http://localhost:8085/change-app-service/callback',
};
