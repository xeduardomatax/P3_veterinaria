"""Convierte las fuentes vectoriales SVG a PNG para su consulta rápida."""
from pathlib import Path
import pymupdf
for nombre in ('modelo', 'arquitectura'):
    ruta = Path('diagramas') / nombre
    documento = pymupdf.open(str(ruta.with_suffix('.svg')))
    pagina = documento[0]
    escala = min(2.0, 4200 / max(pagina.rect.width, pagina.rect.height))
    imagen = pagina.get_pixmap(matrix=pymupdf.Matrix(escala,escala),alpha=False)
    imagen.save(str(ruta.with_suffix('.png')))
    print(f'{nombre}.png: {imagen.width} x {imagen.height}')
