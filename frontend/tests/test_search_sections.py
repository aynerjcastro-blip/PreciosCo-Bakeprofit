"""Pruebas de navegador del contrato de búsqueda, sin conexiones a bases compartidas.

Requiere frontend en http://127.0.0.1:5173 y Playwright para Python con Chromium.
Ejecutar: python -m unittest discover -s frontend/tests -p 'test_*.py' -v
"""
import asyncio
import json
import os
import re
import unittest
from urllib.parse import parse_qs, urlparse
from playwright.async_api import Error, async_playwright, expect

BASE_URL = os.environ.get('FRONTEND_TEST_URL', 'http://127.0.0.1:5173')
SECTIONS = [
    {'id': 101, 'name': 'Supermercado', 'active': True, 'parentId': None},
    {'id': 102, 'name': 'Droguería', 'active': True, 'parentId': None},
    {'id': 103, 'name': 'Cuidado facial', 'active': True, 'parentId': None},
]
CATEGORIES = [
    {'id': 201, 'name': 'Granos', 'active': True, 'parentId': 101},
    {'id': 202, 'name': 'Medicamentos', 'active': True, 'parentId': 102},
    {'id': 203, 'name': 'Hidratantes', 'active': True, 'parentId': 103},
]
STORES = [
    {'id': 301, 'name': 'Supermercado de prueba', 'category': 'Supermercado'},
    {'id': 302, 'name': 'Farmacia de prueba', 'category': 'Droguería'},
    {'id': 303, 'name': 'Facial de prueba', 'category': 'Cuidado facial'},
]
SECTOR_SLUGS = ['supermercados', 'droguerias', 'cuidado-facial']


class SearchSectionsBrowserTest(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.driver = await async_playwright().start()
        self.browser = await self.driver.chromium.launch(headless=True)
        self.context = await self.browser.new_context(reduced_motion='reduce')
        self.page = await self.context.new_page()
        self.requests = []
        self.page_errors = []
        self.page.on('pageerror', lambda error: self.page_errors.append(str(error)))
        self.fail_next_categories = False
        self.slow_supermarket = False
        self.empty_filters = False
        await self.page.route('http://localhost:8080/api/**', self.respond_api)

    async def asyncTearDown(self):
        await self.browser.close()
        await self.driver.stop()

    async def respond_api(self, route):
        parsed = urlparse(route.request.url)
        query = parse_qs(parsed.query)
        self.requests.append((parsed.path, query))
        section_id = int(query['sectionId'][0]) if 'sectionId' in query else None
        status = 200
        if parsed.path == '/api/landing/stats':
            response = {'totalProducts': 3, 'totalStores': 3, 'lastUpdate': None,
                        'sections': [dict(section, productCount=1, storeCount=1)
                                     for section in SECTIONS]}
        elif parsed.path == '/api/categories/roots':
            response = SECTIONS
        elif parsed.path == '/api/categories/active':
            if self.fail_next_categories:
                self.fail_next_categories = False
                status, response = 500, {'message': 'Error simulado'}
            else:
                if self.slow_supermarket and section_id == 101:
                    await asyncio.sleep(0.6)
                response = [] if self.empty_filters else [category for category in CATEGORIES
                    if section_id is None or category['parentId'] == section_id]
        elif parsed.path == '/api/stores/active':
            response = [] if self.empty_filters else [store for store in STORES
                if section_id is None or store['id'] - 200 == section_id]
        elif parsed.path == '/api/products/search':
            products = [] if self.empty_filters else [{
                'id': section_id or 100, 'name': 'Producto de prueba',
                'category': 'Categoría de prueba', 'offers': [],
            }]
            if products and getattr(self, 'include_offers', False):
                store = next(store for store in STORES if store['id'] - 200 == section_id)
                products[0].update({
                    'minPrice': 4500, 'maxPrice': 4500,
                    'offers': [{'storeId': store['id'], 'storeName': store['name'], 'value': 4500}],
                })
            response = {'content': products,
                        'page': {'totalElements': len(products), 'totalPages': 1}}
        else:
            response = []
        try:
            await route.fulfill(status=status, content_type='application/json',
                                headers={'Access-Control-Allow-Origin': '*'}, body=json.dumps(response))
        except Error:
            # El cambio de sección cancela las peticiones de la sección anterior.
            if not route.request.is_navigation_request():
                return
            raise

    async def open_sector(self, slug):
        await self.page.goto(f'{BASE_URL}/buscar?sector={slug}')
        await expect(self.page.get_by_text('1 producto', exact=True)).to_be_visible()

    async def test_cada_seccion_carga_solo_sus_filtros_y_envia_section_id(self):
        for position, slug in enumerate(SECTOR_SLUGS):
            await self.open_sector(slug)
            sidebar = self.page.locator('aside')
            await expect(sidebar.get_by_text(CATEGORIES[position]['name'], exact=True)).to_be_visible()
            await expect(sidebar.get_by_text(STORES[position]['name'], exact=True)).to_be_visible()
            for other_position in range(len(SECTIONS)):
                if other_position != position:
                    await expect(sidebar.get_by_text(STORES[other_position]['name'], exact=True)).to_have_count(0)
                    await expect(sidebar.get_by_text(CATEGORIES[other_position]['name'], exact=True)).to_have_count(0)
            searches = [query for path, query in self.requests if path == '/api/products/search']
            self.assertEqual(searches[-1]['sectionId'], [str(SECTIONS[position]['id'])])

    async def test_todas_conserva_sector_y_cambiar_sector_limpia_categoria_y_tienda(self):
        await self.open_sector('supermercados')
        await self.page.get_by_role('radio', name='Granos', exact=True).check()
        await self.page.get_by_role('radio', name='Todas', exact=True).check()
        self.assertIn('sector=supermercados', self.page.url)
        store_filter = self.page.get_by_role('checkbox', name='Supermercado de prueba')
        await store_filter.click()
        await expect(store_filter).to_be_checked()
        await expect(self.page).to_have_url(re.compile('tienda=301'))
        await self.page.get_by_role('button', name='Droguerías', exact=True).click()
        await expect(self.page.locator('aside').get_by_text('Farmacia de prueba')).to_be_visible()
        self.assertNotIn('tienda=', self.page.url)
        await expect(self.page.get_by_role('radio', name='Todas', exact=True)).to_be_checked()
        await self.page.go_back()
        await expect(self.page.locator('aside').get_by_text('Supermercado de prueba')).to_be_visible()

    async def test_url_con_tienda_de_otro_sector_limpia_tienda_y_pagina(self):
        await self.page.goto(f'{BASE_URL}/buscar?sector=droguerias&tienda=301&pagina=7')
        await expect(self.page.get_by_text('1 producto', exact=True)).to_be_visible()
        self.assertNotIn('tienda=', self.page.url)
        self.assertNotIn('pagina=', self.page.url)
        searches = [query for path, query in self.requests if path == '/api/products/search']
        self.assertTrue(searches)
        self.assertTrue(all('storeId' not in query for query in searches))

    async def test_respuesta_tardia_no_restaura_filtros_de_la_seccion_anterior(self):
        self.slow_supermarket = True
        await self.page.goto(f'{BASE_URL}/buscar?sector=supermercados')
        await self.page.get_by_role('button', name='Droguerías', exact=True).click()
        await expect(self.page.locator('aside').get_by_text('Medicamentos', exact=True)).to_be_visible()
        await self.page.wait_for_timeout(800)
        await expect(self.page.locator('aside').get_by_text('Granos', exact=True)).to_have_count(0)
        await expect(self.page.locator('aside').get_by_text('Farmacia de prueba')).to_be_visible()

    async def test_error_de_filtros_ofrece_reintento(self):
        self.fail_next_categories = True
        await self.page.goto(f'{BASE_URL}/buscar?sector=droguerias')
        await expect(self.page.locator('aside').get_by_role('alert')).to_be_visible()
        self.assertFalse(any(path == '/api/products/search' for path, query in self.requests))
        await self.page.get_by_role('button', name='Reintentar', exact=True).click()
        await expect(self.page.get_by_text('1 producto', exact=True)).to_be_visible()

    async def test_movil_y_limpieza_de_busqueda_por_teclado(self):
        await self.page.set_viewport_size({'width': 390, 'height': 844})
        await self.page.goto(f'{BASE_URL}/buscar?sector=cuidado-facial&q=crema')
        await expect(self.page.get_by_text(re.compile('^1 producto para'))).to_be_visible()
        await self.page.get_by_role('button', name='Filtros', exact=True).click()
        await expect(self.page.locator('aside').get_by_text('Facial de prueba')).to_be_visible()
        await self.page.get_by_role('button', name='Limpiar búsqueda', exact=True).focus()
        await self.page.keyboard.press('Enter')
        search = self.page.get_by_role('searchbox', name='Buscar producto por nombre')
        await expect(search).to_have_value('')
        await expect(search).to_be_focused()
        self.assertTrue(await self.page.evaluate('document.documentElement.scrollWidth <= window.innerWidth'))

    async def test_seccion_desconocida_no_hace_busqueda_global(self):
        await self.page.goto(f'{BASE_URL}/buscar?sector=no-existe')
        await expect(self.page.get_by_text('Esta sección no está disponible.', exact=True)).to_be_visible()
        self.assertFalse(any(path == '/api/products/search' for path, query in self.requests))

    async def test_filtros_y_resultados_vacios_tienen_estado_visible(self):
        self.empty_filters = True
        await self.page.goto(f'{BASE_URL}/buscar?sector=droguerias')
        await expect(self.page.get_by_text('No hay categorías activas disponibles.')).to_be_visible()
        await expect(self.page.get_by_text('No hay tiendas activas disponibles.')).to_be_visible()
        await expect(self.page.get_by_text('Sin resultados', exact=True)).to_be_visible()


    async def test_entrar_desde_cada_seccion_con_ofertas_no_queda_en_blanco(self):
        self.include_offers = True
        for section, slug in zip(SECTIONS, SECTOR_SLUGS):
            await self.page.goto(f'{BASE_URL}/secciones/{slug}')
            await self.page.get_by_role('link', name='Buscar y comparar').click()
            await expect(self.page.get_by_text('1 producto', exact=True)).to_be_visible()
            await expect(self.page.get_by_role('link', name='Comparar precios de Producto de prueba')).to_be_visible()
            self.assertEqual([], self.page_errors)
            searches = [query for path, query in self.requests if path == '/api/products/search']
            self.assertTrue(searches)
            self.assertEqual([str(section['id'])], searches[-1].get('sectionId'))

    async def test_error_del_servidor_muestra_estado_y_conserva_la_pagina(self):
        async def fail_search(route):
            await route.fulfill(status=500, content_type='application/json',
                                headers={'Access-Control-Allow-Origin': '*'},
                                body=json.dumps({'message': 'Fallo simulado'}))
        await self.page.route('http://localhost:8080/api/products/search**', fail_search)
        await self.page.goto(f'{BASE_URL}/buscar?sector=cuidado-facial')
        await expect(self.page.get_by_text('Algo salió mal', exact=True)).to_be_visible()
        await expect(self.page.get_by_role('searchbox')).to_be_visible()
        self.assertEqual([], self.page_errors)


if __name__ == '__main__':
    unittest.main()
