from decimal import Decimal
from datetime import datetime
from unittest.mock import Mock, patch
import pytest
from comun.contenido_vtex import contenido_publicado, peso_venta_kg
from comun.vtex import VtexStrategy
from comun.presentacion import leer_presentacion
from comun.staging import insertar_ofertas

@pytest.mark.parametrize("name,unit,amount,total", [
    ("Salchicha 500 G X16 Unds", "Gramo", "500", "500"),
    ("Mortadela 225 G X13 Unds", "Gramo", "225", "225"),
    ("Colanta x6 Und x1100", "Mililitro", "6600", "6600"),
    ("Coca Cola 250ML SIXPACK", "Mililitro", "1500", "1500"),
    ("Atun 160g", "Gramo", "104", "104"),
])
def test_olimpica_prioriza_especificacion(name, unit, amount, total):
    p = {"productName": name, "measurement_unit": [unit], "unit_multiplier": [amount]}
    assert VtexStrategy("https://www.olimpica.com", tienda="olimpica").tamano(p, {})[0] == Decimal(total)

def test_jumbo_convertido_desde_kg_y_litros():
    assert contenido_publicado({"measurement_unit_un": ["gr"], "unit_multiplier_un": ["0.104"]}, "jumbo") == (Decimal("104"), "G")
    assert contenido_publicado({"measurement_unit_un": ["ml"], "unit_multiplier_un": ["6.6"]}, "jumbo") == (Decimal("6600"), "ML")

@pytest.mark.parametrize("amount", ["NaN", "Infinity", "0", "-1", "abc", None])
def test_dato_estructurado_invalido_no_se_adivina(amount):
    p = {"productName": "Salchicha 500 G X16 Unds", "measurement_unit": ["Gramo"], "unit_multiplier": [amount]}
    assert VtexStrategy("https://www.olimpica.com", tienda="olimpica").tamano(p, {}) == (None, None)

def test_nombre_como_respaldo_y_grx():
    assert VtexStrategy("https://d1.com").tamano({"productName": "Arroz 500 g"}, {}) == (Decimal("500"), "G")
    assert leer_presentacion("ALGODON MK * 50 GRX", "G") == (Decimal("50"), "G")
    assert leer_presentacion("ALGODON 50 GXYZ", "G") == (None, None)

def test_sku_y_peso_crudos_sin_convertir_precio():
    p = {"productName": "Pechuga 900 g", "items": [{"itemId": "123", "ean": "2000123456782", "measurementUnit": "kg", "unitMultiplier": 1.4}]}
    row = VtexStrategy("https://jumbo.com").construir_fila(p, {"id": 1, "store_id": 2}, datetime.now(), (Decimal("33572"), Decimal("23980")))
    assert row[5] == Decimal("33572")
    assert row[13:] == ("123", Decimal("1.4"))
    assert peso_venta_kg({"measurementUnit": "un", "unitMultiplier": 1}) is None

def test_writer_admite_paginas_y_vtex_con_misma_corrida():
    conn = Mock()
    conn.cursor.return_value.__enter__ = Mock(return_value=Mock())
    conn.cursor.return_value.__exit__ = Mock(return_value=False)
    with patch("comun.staging.psycopg2.extras.execute_values") as insert:
        insertar_ofertas(conn, [tuple(range(13)), tuple(range(15))], "run")
        rows = insert.call_args.args[2]
        assert all(len(row) == 16 and row[-1] == "run" for row in rows)
        assert rows[0][-3:-1] == (None, None)


def test_respaldo_no_confunde_piezas_y_reconoce_sixpack():
    from comun.tamanos import parsear_tamano
    assert parsear_tamano("Salchicha 500 G X16 Unds") == (None, None)
    assert parsear_tamano("Atun 80 G X3 Unds") == (None, None)
    assert parsear_tamano("Coca-Cola 250ML SIXPACK") == (Decimal("1500"), "ML")
    assert parsear_tamano("Pack de 3 Atun 80 G X3 Unds") == (Decimal("240"), "G")
    assert parsear_tamano("Leche 6 x 1000 ml") == (Decimal("6000"), "ML")


def test_olimpica_peso_del_empaque_desde_ficha():
    p = {"productName": "Pechuga", "measurement_unit": ["Gramo"], "unit_multiplier": ["1000"],
         "items": [{"itemId": "pollo", "measurementUnit": "un", "unitMultiplier": 1}]}
    row = VtexStrategy("https://www.olimpica.com", tienda="olimpica").construir_fila(
        p, {"id": 1, "store_id": 2}, datetime.now(), (Decimal("23980"), None))
    assert row[-1] == Decimal("1")


def test_seleccion_sin_targets_no_simula_exito():
    from comun.corrida import ejecutar
    with patch("comun.corrida.conectar") as conn, patch("comun.corrida.nueva_sesion"), \
            patch("comun.corrida.leer_targets", return_value=[]), \
            patch("comun.corrida.iniciar_corrida") as begin:
        assert ejecutar("Supermercado", Mock()) == 2
        begin.assert_not_called()
        conn.return_value.close.assert_called_once()


@pytest.mark.parametrize("published, expected", [("8214", "8214"), (None, None), ("", None)])
def test_facial_conserva_sku_publicado(published, expected):
    from facial.scraper_cuidado_facial import extraerSku
    assert extraerSku({"items": [{"itemId": published}]}) == expected
    assert extraerSku({"items": []}) is None
