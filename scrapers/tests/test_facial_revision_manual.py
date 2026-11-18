import csv
from unittest.mock import Mock, patch

import pytest

from facial import scraper_cuidado_facial as facial
from comun.staging import normalizarFila


@pytest.fixture(autouse=True)
def limpiarDiagnosticos():
    for filas in (facial.SKU_OFFERS, facial.CONFLICTOS_TAMANOS, facial.SIN_TAMANO,
                  facial.DESCARTADOS):
        filas.clear()
    facial.EAN_DIAG.clear()


def productoPublicado(codigo=None, sku="7012"):
    return {
        "productId": "producto-prueba",
        "productName": "Crema facial 30 ml",
        "brand": "Marca de prueba",
        "link": "https://example.test/crema-30-ml/p",
        "items": [{"itemId": sku, "ean": codigo, "images": [],
                   "sellers": [{"commertialOffer": {
                       "IsAvailable": True, "Stock": 3, "Price": 10000}}]}],
    }


@pytest.mark.parametrize("codigo", [None, "8214", "0000000000000", "incorrecto"])
def test_sin_ean_comercial_guarda_sku_y_codigo_original(codigo):
    producto = productoPublicado(codigo)
    producto["productReference"] = "4006381333931"
    producto["items"][0]["images"] = [{"imageUrl": "https://example.test/4006381333931.jpg"}]
    oferta, motivo = facial.parsear(producto)
    assert motivo is None
    assert oferta["ean"] == "7012"
    assert oferta["has_ean"] is False
    assert oferta["original_code"] == codigo


def test_sku_con_checksum_no_se_convierte_en_identidad_comercial():
    oferta, motivo = facial.parsear(productoPublicado(sku="4006381333931"))
    assert motivo is None
    assert oferta["ean"] == "4006381333931"
    assert oferta["has_ean"] is False


def test_ean_comercial_publicado_conserva_su_origen():
    oferta, motivo = facial.parsear(productoPublicado("4006381333931"))
    assert motivo is None
    assert oferta["ean"] == "4006381333931"
    assert oferta["has_ean"] is True


@pytest.mark.parametrize("sku", ["sku-con-letras", "s" * 100])
def test_writer_conserva_sku_alfanumerico_y_largo(sku):
    oferta, motivo = facial.parsear(productoPublicado(sku=sku))
    assert motivo is None
    assert oferta["ean"] == (sku if len(sku) <= 20 else None)
    assert oferta["status"] == ("PENDING" if len(sku) <= 20 else "CONFLICT")
    fila = tuple(range(15)) + (False, None)
    assert len(normalizarFila(fila, None)) == 18


def test_fuentes_contradictorias_no_votan_ni_priorizan_nombre():
    producto = productoPublicado("4006381333931")
    producto["productTitle"] = "Crema facial 50 ml"
    producto["link"] = "https://example.test/crema-50-ml/p"
    oferta, motivo = facial.parsear(producto)
    assert motivo is None
    assert oferta["net_content"] is None and oferta["unit_base"] is None
    assert oferta["tamano_detalle"] == "conflicto"
    assert oferta["status"] == "CONFLICT"
    assert "conflicto de tamaño" in oferta["reason"]
    evidencia = oferta["size_evidence"]
    assert {dato["detected_content"] for dato in evidencia} == {30, 50}


def test_gramos_y_mililitros_son_conflicto_aunque_coincida_numero():
    producto = productoPublicado()
    producto["items"][0]["name"] = "Crema facial 30 g"
    assert facial.extraer_tamano(producto) == (None, None, "conflicto")


def test_descripcion_etiquetada_tambien_detecta_contradiccion():
    producto = productoPublicado()
    producto["description"] = "Contenido neto: 50 ml"
    assert facial.extraer_tamano(producto) == (None, None, "conflicto")


def test_fuentes_coherentes_aceptan_una_presentacion():
    producto = productoPublicado()
    producto["items"][0]["name"] = "Crema facial 30 ml"
    assert facial.extraer_tamano(producto) == (30, "ML", "nombre")


def test_diagnosticos_solo_incluyen_lote_confirmado(tmp_path, monkeypatch):
    producto = productoPublicado("8214")
    producto["items"][0]["name"] = "Crema facial 50 ml"
    objetivo = {"id": 1, "store_id": 2, "store_name": "bellapiel", "search_term": "crema",
                "category_name": "Hidratantes", "store_category_fq": "C:/1/"}
    cliente = Mock()
    cliente.descargar.return_value = [producto]
    with patch.object(facial, "insertar_ofertas") as insertar:
        resumen = facial.procesar_target(Mock(), cliente, objetivo, "corrida-prueba")
    assert resumen["insertados"] == 1
    fila = insertar.call_args.args[1][0]
    assert fila[2] == "7012" and fila[9:11] == (None, None)
    assert fila[12] == "CONFLICT" and fila[15] is False
    assert "conflicto de tamaño" in fila[16]
    monkeypatch.setenv("SKU_OFFERS_PATH", str(tmp_path / "sku_offers.csv"))
    monkeypatch.setenv("CONFLICTOS_TAMANOS_PATH", str(tmp_path / "conflictos_tamanos.csv"))
    facial.guardarRevisionManual()
    with (tmp_path / "sku_offers.csv").open(encoding="utf-8-sig") as archivo:
        filas = list(csv.DictReader(archivo))
    assert filas[0]["store_sku"] == "7012" and filas[0]["local_code"] == "8214"
    with (tmp_path / "conflictos_tamanos.csv").open(encoding="utf-8-sig") as archivo:
        conflictos = list(csv.DictReader(archivo))
    assert {fila["detected_content"] for fila in conflictos} == {"30.0", "50.0"}
    facial.SKU_OFFERS.clear()
    facial.CONFLICTOS_TAMANOS.clear()
    import psycopg2
    with patch.object(facial, "insertar_ofertas", side_effect=psycopg2.Error("fallo de prueba")):
        assert facial.procesar_target(Mock(), cliente, objetivo)["insertados"] == 0
    assert not facial.SKU_OFFERS and not facial.CONFLICTOS_TAMANOS
