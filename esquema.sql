PRAGMA foreign_keys = ON;

CREATE TABLE cafes_origen (
  _id         INTEGER PRIMARY KEY AUTOINCREMENT,
  variedad    TEXT NOT NULL,
  origen      TEXT,
  altitud     INTEGER,
  proceso     TEXT,
  tueste      TEXT,
  aroma       TEXT,
  descripcion TEXT
);

CREATE TABLE marcas (
  _id         INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre      TEXT NOT NULL UNIQUE,
  etiquetas   TEXT,
  es_nacional INTEGER NOT NULL DEFAULT 1,
  descripcion TEXT,
  inicial     TEXT
);

CREATE TABLE cafeterias (
  _id              INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre           TEXT NOT NULL,
  zona             TEXT,
  barrio           TEXT,
  direccion        TEXT,
  horario          TEXT,
  tipo             TEXT,
  marca_id         INTEGER REFERENCES marcas(_id),
  lat              REAL,
  lng              REAL,
  revisar_ubicacion INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE cafeteria_cafes_origen (
  cafeteria_id INTEGER REFERENCES cafeterias(_id),
  cafe_id      INTEGER REFERENCES cafes_origen(_id),
  PRIMARY KEY (cafeteria_id, cafe_id)
);

CREATE TABLE tostadurias (
  _id               INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre            TEXT NOT NULL,
  region            TEXT,
  direccion         TEXT,
  horarios          TEXT,
  descripcion       TEXT,
  contacto          TEXT,
  lat               REAL,
  lng               REAL,
  revisar_ubicacion INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE productores (
  _id          INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre_finca TEXT NOT NULL,
  familia      TEXT,
  origen       TEXT,
  num_cafes    INTEGER,
  altitud      INTEGER,
  descripcion  TEXT,
  lat          REAL,
  lng          REAL
);

CREATE TABLE puntos_de_venta (
  _id       INTEGER PRIMARY KEY AUTOINCREMENT,
  marca     TEXT NOT NULL,
  local     TEXT,
  barrio    TEXT,
  direccion TEXT,
  lat       REAL,
  lng       REAL
);
