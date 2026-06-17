FROM nginx:alpine
COPY dist/iam-web-service/browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 4200
CMD ["nginx", "-g", "daemon off;"]
