import sqlite3, os

paths = [
    'app/src/main/assets/Furgones.db',
    'desktop/src/main/resources/Furgones.db',
    'shared/src/commonMain/resources/Furgones.db'
]

for p in paths:
    if os.path.exists(p):
        conn = sqlite3.connect(p)
        conn.execute("UPDATE Usuarios SET Password = '12345' WHERE IdUsuario IN ('ADMIN', 'SUPERVISOR', 'OPERARIO', 'TECNICO')")
        conn.execute("UPDATE Usuarios SET Password = 'SuperUser123' WHERE IdUsuario = 'DESARROLLADOR'")
        conn.commit()
        print(f"Updated passwords in {p}:", conn.execute("SELECT IdUsuario, Password FROM Usuarios").fetchall())
        conn.close()
