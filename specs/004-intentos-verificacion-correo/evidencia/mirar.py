import sys, json
from playwright.sync_api import sync_playwright
out = sys.argv[1]
casos = {"2-intentos":"222222","1-intento":"111111","0-intentos":"000000","sin-dato":"333333","too-many":"999999","error-generico":"555555","fallo-red":"777777"}
vistas = [("movil-claro", 390, 780, "light"), ("escritorio-oscuro", 1280, 800, "dark")]
res = {}
with sync_playwright() as p:
    b = p.chromium.launch()
    for nombre, w, h, tema in vistas:
        ctx = b.new_context(viewport={"width": w, "height": h}, color_scheme=tema)
        ctx.add_cookies([{"name": "lmc_sesion", "value": "simulada", "url": "http://localhost:3003"}])
        pg = ctx.new_page()
        for caso, codigo in casos.items():
            if nombre != "movil-claro" and caso not in ("2-intentos", "0-intentos"): continue
            pg.goto("http://localhost:3003/panel/verificar")
            pg.locator('input[name="codigo"]').fill(codigo)
            pg.get_by_role("button", name="Verificar").click()
            pg.wait_for_timeout(1500)
            main = pg.locator("main")
            res[f"{nombre}/{caso}"] = {
                "alert": [e.inner_text() for e in main.locator('[role="alert"]').all()],
                "texto_main": main.inner_text()[-260:],
            }
            pg.screenshot(path=f"{out}/{nombre}-{caso}.png", full_page=True)
        ctx.close()
    b.close()
open(f"{out}/resultados.json", "w", encoding="utf-8").write(json.dumps(res, ensure_ascii=False, indent=1))
