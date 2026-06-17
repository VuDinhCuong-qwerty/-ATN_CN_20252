FROM nginx:alpine
COPY dist/change-web-service/browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 4201
CMD ["nginx", "-g", "daemon off;"]
