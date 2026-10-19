import re

file_path = "app/src/main/java/org/claudroide/app/core/design/theme_compat/Type.kt"
with open(file_path, "r") as f:
    code = f.read()

code = re.sub(r'public val Inter: FontFamily = FontFamily\(.*?\)', 'public val Inter: FontFamily = FontFamily.Default', code, flags=re.DOTALL)
code = re.sub(r'public val JetBrainsMono: FontFamily = FontFamily\(.*?\)', 'public val JetBrainsMono: FontFamily = FontFamily.Monospace', code, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(code)
