# Versões de prescrição: descarte, arquivamento e cópia

Rotas sob `/api/v1`, todas só para o nutricionista autor da prescrição (vínculo ativo).

- `DELETE /prescription-versions/{versionId}`: descarta um rascunho. A versão é removida de fato
  (refeições, itens, substituições e metas saem em cascata) e o número dela fica livre. Se não
  sobrar nenhuma versão, a prescrição inteira é apagada e `POST
  /nutritionists/me/patients/{patientId}/prescriptions` volta a funcionar. Respostas: `204`;
  `409 PRESCRIPTION_CONFLICT` se a versão não for rascunho; `403` e `404` como nas demais rotas.
- `POST /prescription-versions/{versionId}/archive`: só versão `PUBLISHED`. Em `DRAFT` ou
  `ARCHIVED` responde `409 PRESCRIPTION_CONFLICT`. Para abandonar um rascunho, use o descarte.
- `POST /prescriptions/{prescriptionId}/versions`: o novo rascunho copia a versão `PUBLISHED`;
  se não houver, a que foi publicada mais recentemente (maior `publishedAt`). Nunca copia um
  rascunho arquivado. Sem nenhuma versão já publicada não há o que copiar e a rota responde
  `409 PRESCRIPTION_CONFLICT` (estado inalcançável pela API: a prescrição nasce com um rascunho e
  descartá-lo apaga a prescrição).

Sem migration: as FKs de `prescribed_meal`, `prescribed_meal_item`, `allowed_substitution` e
`prescription_nutrition_target` já são `ON DELETE CASCADE`, e os gatilhos de imutabilidade só
bloqueiam o que não é rascunho (coberto por `PrescriptionMigrationTest`).
