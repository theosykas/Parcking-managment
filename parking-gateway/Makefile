COMPOSE = docker compose
MVNW = ./mvnw

all: up

up:
	$(COMPOSE) up -d

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs -f

stop-serveur:
	$(COMPOSE) stop cannes-api

down:
	$(COMPOSE) down

test:
	$(MVNW) test

install:
	$(MVNW) clean install

compile:
	$(MVNW) clean compile

run-serveur:
	$(MVNW) spring-boot:run

.PHONY: all up ps logs stop-serveur start-serveur clean re run test