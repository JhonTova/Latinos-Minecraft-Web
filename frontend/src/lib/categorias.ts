/**
 * Texto propio de cada modalidad.
 *
 * Existe porque una página por categoría sin contenido propio no es una página:
 * es la misma lista con un filtro puesto, y Google la trata como duplicada de
 * `/servidores`. Lo que la convierte en algo que puede posicionar es este texto
 * —escrito, no generado con plantilla— más un título y una descripción que
 * digan lo que la gente escribe en el buscador.
 *
 * Nadie busca «modalidad SKYBLOCK». Busca «servidores de skyblock español».
 *
 * Los códigos son los de la tabla `game_modes`. Si se añade una modalidad allí
 * y no aquí, la página sigue funcionando con un texto genérico — pero no
 * posicionará, así que conviene escribirle el suyo.
 */
export interface Categoria {
  /** Lo que aparece en la pestaña y en Google. Lo específico primero. */
  titulo: string;
  descripcion: string;
  /** Encabezado de la página. Distinto del título: aquí ya se sabe dónde estás. */
  h1: string;
  /** Dos o tres frases de verdad. Ni relleno ni palabras repetidas. */
  intro: string[];
}

export const CATEGORIAS: Record<string, Categoria> = {
  SURVIVAL: {
    titulo: "Servidores de Minecraft Survival en español",
    descripcion:
      "Los mejores servidores survival de Minecraft en español y latinos. Ranking con votos verificados, IP para entrar al momento y jugadores conectados en tiempo real.",
    h1: "Servidores de Survival",
    intro: [
      "El survival es Minecraft como se jugó siempre: empiezas sin nada, picas, construyes y sobrevives. En un servidor, además, con más gente alrededor — y ahí es donde cambia todo, porque tus vecinos pueden ser tu mejor alianza o tu peor problema.",
      "Los de esta lista están ordenados por los votos de los últimos siete días, así que arriba está lo que la gente juega ahora, no lo que era popular hace dos años. Mira los jugadores conectados antes de entrar: un survival vacío se siente muy distinto a uno con cincuenta personas.",
    ],
  },
  SKYBLOCK: {
    titulo: "Servidores de Minecraft SkyBlock en español",
    descripcion:
      "Servidores skyblock de Minecraft en español y latinos. Ranking honesto por votos verificados, con IP, versión y jugadores conectados.",
    h1: "Servidores de SkyBlock",
    intro: [
      "Empiezas en una isla flotante con un árbol, un cofre y nada más. Todo lo que llegue a existir lo tienes que fabricar tú a partir de eso. Es el modo que más engancha a quien le gusta optimizar: cada bloque cuenta y cada error se paga.",
      "Casi todos los skyblock añaden lo suyo —tienda, misiones, islas en equipo, niveles de isla— así que merece la pena mirar dos o tres antes de quedarse. Los de arriba son los más votados de la última semana.",
    ],
  },
  PVP: {
    titulo: "Servidores de Minecraft PvP en español",
    descripcion:
      "Servidores PvP de Minecraft en español y latinos: kitpvp, practice y arenas. Ranking por votos verificados con IP y jugadores conectados.",
    h1: "Servidores de PvP",
    intro: [
      "Aquí se viene a pelear. Kits preparados, arenas, rankings de kills y partidas que empiezan en cuanto entras. Sin picar durante horas para poder jugar.",
      "El PvP es de las modalidades donde más importa el ping: un servidor con jugadores de tu región se siente completamente distinto a uno alojado al otro lado del mundo. Los que salen aquí son los más votados por gente que juega en español.",
    ],
  },
  FACTIONS: {
    titulo: "Servidores de Minecraft Factions en español",
    descripcion:
      "Servidores de factions en español y latinos. Ranking por votos verificados, con IP, versión y jugadores conectados en directo.",
    h1: "Servidores de Factions",
    intro: [
      "Montas un clan, reclamas terreno, lo defiendes y asaltas el de los demás. Factions es tanto de organizarse con gente como de jugar: las bases grandes no las levanta nadie solo, y las guerras se ganan por coordinación más que por equipo.",
      "Si vienes de jugar solo, entra en el Discord del servidor antes que al servidor. En factions, estar en un clan desde el primer día es la diferencia entre durar y que te arrasen la primera noche.",
    ],
  },
  PRISON: {
    titulo: "Servidores de Minecraft Prison en español",
    descripcion:
      "Servidores prison de Minecraft en español y latinos. Ranking por votos verificados con IP, versión y jugadores conectados.",
    h1: "Servidores de Prison",
    intro: [
      "Empiezas en la celda A y vas subiendo de rango picando y vendiendo. Cada rango abre una mina mejor, y el bucle —picar, vender, ascender— es de los más adictivos que tiene Minecraft.",
      "Los prison suelen tener economías muy trabajadas, con encantamientos propios y prestigios que reinician el progreso a cambio de ventajas. Los de la lista están ordenados por los votos de la última semana.",
    ],
  },
  MINIGAMES: {
    titulo: "Servidores de minijuegos de Minecraft en español",
    descripcion:
      "Servidores de minijuegos de Minecraft en español y latinos: partidas rápidas, sin progreso que mantener. Ranking por votos verificados.",
    h1: "Servidores de Minijuegos",
    intro: [
      "Partidas cortas, sin nada que cuidar entre sesión y sesión. Entras, juegas veinte minutos y te vas — y al día siguiente no has perdido nada por no haber entrado.",
      "Es lo que mejor funciona para jugar con amigos que no tienen tu misma disponibilidad. Mira los jugadores conectados: en minijuegos, un servidor vacío no es que se juegue peor, es que directamente no arrancan las partidas.",
    ],
  },
  BEDWARS: {
    titulo: "Servidores de BedWars de Minecraft en español",
    descripcion:
      "Servidores de bedwars en español y latinos. Ranking por votos verificados, con IP y jugadores conectados en tiempo real.",
    h1: "Servidores de BedWars",
    intro: [
      "Proteges tu cama y rompes la de los demás. Mientras tengas cama reapareces; sin ella, una muerte te elimina. Esa regla tan simple es la que convierte cada partida en una decisión constante entre atacar y quedarte a defender.",
      "Se juega en equipos, así que es de lo mejor para entrar con amigos. Los de aquí son los más votados por la comunidad hispanohablante en los últimos siete días.",
    ],
  },
  SKYWARS: {
    titulo: "Servidores de SkyWars de Minecraft en español",
    descripcion:
      "Servidores skywars en español y latinos. Ranking por votos verificados, con IP, versión y jugadores conectados.",
    h1: "Servidores de SkyWars",
    intro: [
      "Cada uno empieza en su isla con un cofre. Puentes al centro, todos contra todos y partidas que se deciden en cinco minutos. Es de lo más rápido que se puede jugar en Minecraft.",
      "Lo que separa a un buen servidor de skywars de uno mediocre es el equilibrio de los cofres y la cantidad de mapas. Los de arriba son los que más gente vota.",
    ],
  },
  ANARCHY: {
    titulo: "Servidores de Minecraft Anarquía en español",
    descripcion:
      "Servidores de anarquía en español y latinos: sin reglas, sin protecciones, sin resets. Ranking por votos verificados.",
    h1: "Servidores de Anarquía",
    intro: [
      "Sin reglas, sin protecciones y casi siempre sin reinicios. Lo que construyas puede desaparecer, y lo que encuentres es tuyo hasta que alguien te lo quite. No es para todo el mundo, y ese es justamente el atractivo.",
      "En anarquía el mundo lleva años acumulando historia: bases abandonadas, carreteras del Nether que hizo gente que ya no juega, ruinas de guerras que pasaron antes de que llegaras. Entra sabiendo que vas a perder cosas.",
    ],
  },
  CREATIVE: {
    titulo: "Servidores creativos de Minecraft en español",
    descripcion:
      "Servidores creativos de Minecraft en español y latinos, con plots para construir. Ranking por votos verificados con IP.",
    h1: "Servidores Creativos",
    intro: [
      "Bloques infinitos, vuelo y una parcela para ti. Sin monstruos, sin hambre y sin nada que te interrumpa mientras construyes.",
      "La mayoría dan una parcela al entrar y permiten invitar a otros a la tuya, así que funcionan muy bien para construir en grupo o para probar ideas antes de llevarlas a un survival.",
    ],
  },
  ROLEPLAY: {
    titulo: "Servidores de rol de Minecraft en español",
    descripcion:
      "Servidores de roleplay de Minecraft en español y latinos. Ranking por votos verificados, con IP y jugadores conectados.",
    h1: "Servidores de Roleplay",
    intro: [
      "Aquí no interpretas a un jugador: interpretas a un personaje. Hay ciudades con normas, oficios, política y a veces una historia que avanza según lo que hagan los que están dentro.",
      "Casi todos piden leerse las normas antes de entrar y algunos tienen proceso de admisión. No es burocracia por gusto: un rol se sostiene porque todos siguen las mismas reglas.",
    ],
  },
  TOWNY: {
    titulo: "Servidores Towny de Minecraft en español",
    descripcion:
      "Servidores towny de Minecraft en español y latinos: ciudades, naciones y economía. Ranking por votos verificados.",
    h1: "Servidores de Towny",
    intro: [
      "Fundas un pueblo, reclamas terreno, cobras impuestos y te alías con otras ciudades para formar naciones. Es factions con burocracia — y esa burocracia es exactamente lo divertido para quien le gusta el juego político.",
      "Funciona mejor cuanta más gente hay, porque el interés está en negociar con otras ciudades. Mira los jugadores conectados antes de fundar nada.",
    ],
  },
  EARTH: {
    titulo: "Servidores Earth y SMP de Minecraft en español",
    descripcion:
      "Servidores earth y SMP en español y latinos, con el mapa del mundo real a escala. Ranking por votos verificados.",
    h1: "Servidores Earth / SMP",
    intro: [
      "El mapa del mundo real a escala. Puedes empezar en tu propio país, construir tu ciudad donde vives y encontrarte con vecinos que están, literalmente, al lado.",
      "Los SMP son survival multijugador con una comunidad más cerrada y menos plugins: menos tienda y más gente. Los dos comparten lo mismo — el interés está en quién juega, no en las mecánicas.",
    ],
  },
  MODDED: {
    titulo: "Servidores de Minecraft con mods en español",
    descripcion:
      "Servidores de Minecraft con mods y modpacks en español y latinos. Ranking por votos verificados, con IP y versión.",
    h1: "Servidores con Mods",
    intro: [
      "Máquinas, magia, dimensiones nuevas y cientos de bloques que el juego base no tiene. Un servidor con mods es otro juego montado encima de Minecraft.",
      "Necesitas instalar el mismo modpack que use el servidor, así que mira la versión y el pack antes de intentar entrar. Casi todos lo indican en su ficha o en su Discord.",
    ],
  },
  PARKOUR: {
    titulo: "Servidores de Parkour de Minecraft en español",
    descripcion:
      "Servidores de parkour de Minecraft en español y latinos. Ranking por votos verificados con IP y jugadores conectados.",
    h1: "Servidores de Parkour",
    intro: [
      "Saltos, precisión y repetir el mismo tramo hasta que sale. El parkour es de lo poco en Minecraft donde solo compites contra el reloj y contra ti mismo.",
      "Los buenos tienen decenas de mapas por dificultad y tablas de récords. Si vas empezando, busca los que marcan los niveles: entrar directo a un mapa experto es la forma más rápida de dejarlo.",
    ],
  },
  ECONOMY: {
    titulo: "Servidores de Minecraft con economía en español",
    descripcion:
      "Servidores de Minecraft con economía en español y latinos: tiendas, subastas y comercio entre jugadores. Ranking por votos verificados.",
    h1: "Servidores con Economía",
    intro: [
      "Dinero, tiendas de jugador, subastas y precios que los ponen los propios jugadores. Aquí montar un negocio puede ser tan entretenido como picar.",
      "Merece la pena mirar si la economía la mueven los jugadores o el servidor: cuando todo se compra a un aldeano con precio fijo, el comercio entre personas deja de tener sentido.",
    ],
  },
};

/** Texto de respaldo para una modalidad del catálogo sin copia propia todavía. */
export function categoriaDe(codigo: string, nombre: string): Categoria {
  return (
    CATEGORIAS[codigo] ?? {
      titulo: `Servidores de Minecraft ${nombre} en español`,
      descripcion: `Servidores de Minecraft ${nombre} en español y latinos. Ranking por votos verificados, con IP, versión y jugadores conectados.`,
      h1: `Servidores de ${nombre}`,
      intro: [
        `Todos los servidores de ${nombre} del directorio, ordenados por los votos verificados de los últimos siete días.`,
      ],
    }
  );
}
