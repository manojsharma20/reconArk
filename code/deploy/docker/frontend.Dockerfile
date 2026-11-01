# The built frontend (npm run build in frontend/) served by nginx, which also routes /api calls to the services.
FROM nginxinc/nginx-unprivileged:1.31-alpine
COPY deploy/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend/dist /usr/share/nginx/html
