CREATE TABLE grupos (id TEXT NOT NULL, grupo TEXT, creador TEXT NOT NULL , primary key (id,creador));
CREATE TABLE contactos (id TEXT NOT NULL, contacto TEXT, alias TEXT, creador TEXT NOT NULL, primary key (id,creador));
CREATE TABLE mensajes (numero INTEGER PRIMARY KEY autoincrement, fecha TEXT, tipo INT, mensaje TEXT, creador TEXT NOT NULL);
CREATE TABLE miscelaneo (numero INTEGER PRIMARY KEY NOT NULL, atributo TEXT, valor TEXT);
CREATE TABLE direcciones_arranque (numero INTEGER PRIMARY KEY NOT NULL, ip STRING, puerto INT);

