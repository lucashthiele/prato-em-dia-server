# Prato em Dia — Servidor

API que conecta nutricionistas e pacientes: o nutricionista publica uma prescrição, o paciente planeja a semana e registra o que realmente comeu.

## Language

### Prescrição

**Prescrição**:
Conjunto versionado de orientações alimentares que um nutricionista publica para um paciente. Cada versão descreve um único dia típico.
_Avoid_: Dieta, plano alimentar

**Modo da prescrição**:
Regra que vale para a prescrição inteira e define o que o paciente pode registrar: `RESTRICTED` (só os alimentos prescritos), `SUBSTITUTIONS_ONLY` (prescritos e substituições autorizadas) ou `FREE` (qualquer alimento ativo do catálogo).
_Avoid_: Tipo, nível de liberdade

**Categoria**:
Uma das quatro refeições fixas de todo dia: café da manhã, almoço, lanche e janta. Não tem horário planejado.
_Avoid_: Refeição (quando se quer dizer o tipo), slot

### Planejamento

**Plano semanal**:
Distribuição das refeições de um paciente por dias, criada e editada por ele a partir da prescrição. O paciente pode regenerá-lo ou excluí-lo, nunca alterar a prescrição.
_Avoid_: Cardápio

**Refeição planejada**:
A categoria de um dia dentro de um plano semanal, com seus itens planejados.
_Avoid_: Refeição prescrita (que pertence à prescrição)

**Refeição substituída**:
Refeição planejada de um dia que uma regeneração posterior cobriu. Fica como histórico, e suas execuções não contam para o plano ativo.
_Avoid_: Plano antigo, plano inativo

### Registro

**Execução**:
Registro editável do que o paciente consumiu em uma categoria e dia, com horário e itens do catálogo. Pode existir sem plano. Uma categoria pode ter várias execuções no mesmo dia.
_Avoid_: Conclusão, check-in, log

**Estado da categoria**:
Situação derivada do registro de uma categoria em um dia: A registrar, Parcial ou Registrada. Só existe Parcial nos modos restritos, onde a cobertura dos itens planejados é exigida.
_Avoid_: Status da refeição

**Item coberto**:
Item planejado que alguma execução ativa da mesma refeição registrou (diretamente ou por substituição autorizada).
_Avoid_: Item concluído

### Compras

**Lista de compras**:
Instantâneo, por plano semanal, dos alimentos prescritos somados por alimento, porção e unidade. Não decompõe pratos em ingredientes.
_Avoid_: Carrinho, lista de ingredientes
