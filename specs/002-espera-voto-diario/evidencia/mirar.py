import sys, json
from playwright.sync_api import sync_playwright
out = sys.argv[1]
casos = ["ca7-diario","ca9-diario-sin-tiempo","ca9-diario-invalido","ca10-ventana","ca10-ventana-sin-tiempo","ca11-otro","ca11-no-json"]
res = {}
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": 420, "height": 760})
    for c in casos:
        pg.goto(f"http://localhost:3002/votar/{c}")
        pg.get_by_role("button", name="Votar").first.click()
        pg.wait_for_timeout(1500)
        main = pg.locator("main")
        r = {
          "texto": main.inner_text(),
          "status": [e.inner_text() for e in pg.locator('main [role="status"]').all()],
          "alert": [e.inner_text() for e in pg.locator('main [role="alert"]').all()],
          "text_danger": pg.locator("main .text-danger").count(),
          "boton_votar": pg.get_by_role("button", name="Votar").count(),
          "detail_visible": "NO debe mostrarse" in main.inner_text(),
        }
        res[c] = r
        pg.screenshot(path=f"{out}/{c}.png", full_page=True)
    b.close()
open(f"{out}/resultados.json","w",encoding="utf-8").write(json.dumps(res, ensure_ascii=False, indent=1))
