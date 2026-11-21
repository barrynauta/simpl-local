# Each request resolves the backend name again, so the replicas share the load.
set $upstream backend:8080;
proxy_pass http://$upstream;
proxy_http_version 1.1;
proxy_set_header Host $http_host;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $http_host;
proxy_set_header Connection "";
proxy_request_buffering on;
proxy_read_timeout 120s;
proxy_send_timeout 120s;
proxy_connect_timeout 5s;
