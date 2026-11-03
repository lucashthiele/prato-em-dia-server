FROM postgres:17-alpine
RUN apk add --no-cache age rclone curl tzdata
COPY lib.sh backup.sh restore-test.sh entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/lib.sh /usr/local/bin/backup.sh /usr/local/bin/restore-test.sh /usr/local/bin/entrypoint.sh
ENV TZ=America/Sao_Paulo
ENTRYPOINT []
CMD ["/usr/local/bin/entrypoint.sh"]
