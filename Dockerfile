# Serves the Fight game in a browser terminal using ttyd.
# Each visitor who opens the page gets their own `java Fight` process to play.

FROM debian:bookworm-slim

# Java to run the game, plus tools to fetch the ttyd binary.
RUN apt-get update \
    && apt-get install -y --no-install-recommends default-jdk-headless ca-certificates wget \
    && rm -rf /var/lib/apt/lists/*

# ttyd isn't in Debian's repos, so grab the official self-contained static binary.
RUN wget -O /usr/local/bin/ttyd \
        https://github.com/tsl0922/ttyd/releases/download/1.7.7/ttyd.x86_64 \
    && chmod +x /usr/local/bin/ttyd

WORKDIR /game

# Copy the source in and compile it inside the image.
# -encoding UTF-8 is required because the sources contain emoji and box-drawing chars.
COPY *.java /game/
RUN javac -encoding UTF-8 *.java

# UTF-8 locale so the emoji and box-drawing output render correctly in the browser.
ENV LANG=C.UTF-8

# ttyd serves on 7681. -W makes the terminal writable so players can type.
# Each browser connection spawns a fresh `java Fight`, so players don't collide.
EXPOSE 7681
CMD ["ttyd", "-W", "-p", "7681", "java", "-Dfile.encoding=UTF-8", "Fight"]
