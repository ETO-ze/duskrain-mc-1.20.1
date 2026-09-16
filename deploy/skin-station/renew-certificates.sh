#!/bin/sh
set -eu
/srv/duskrain-skin/certbot-venv/bin/certbot renew --non-interactive --no-random-sleep-on-renew --config-dir /srv/duskrain-skin/tls --work-dir /srv/duskrain-skin/acme-work --logs-dir /srv/duskrain-skin/logs/acme --deploy-hook '/usr/bin/nginx -t && /usr/bin/nginx -s reload'
