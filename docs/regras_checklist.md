# Regras e Fluxo de Resposta do Checklist

Este documento resume as regras de negócio, validações e fluxos identificados para o preenchimento de respostas de checklists no aplicativo Segbar Mobile.

## 1. Regras de Bloqueio/Habilitação de Opções (Transições Permitidas)
As opções de resposta disponíveis para uma pergunta podem depender da **resposta dada na inspeção anterior** (`lastSelectedOptionLabel`). As restrições de seleções são coordenadas pela classe `ChecklistValidationUtils`:

*   **Se não houver resposta anterior (`null`):** 
    *   **Permitidas:** `PV`, `NI`, `NE`.
    *   **Bloqueadas:** Qualquer outra opção.
*   **Se a resposta anterior foi `NE`:**
    *   **Bloqueadas:** `PC`, `AU`, `DM`, `DS`.
*   **Se a resposta anterior foi `PV`:**
    *   **Bloqueadas:** `PV`, `NE`.
*   **Se a resposta anterior foi `AU`, `PC` ou `DM`:**
    *   **Bloqueadas:** `NE`, `PV`.
*   **Se a resposta anterior foi `DS`:**
    *   **Bloqueadas:** `PC`, `AU`, `DM`, `DS`.

## 2. Regras de Obrigatoriedade (Evidências e Anomalias)
Baseado na opção selecionada, a interface do Checklist exige campos adicionais antes de permitir o envio final ("Submit"):

*   **Para a opção `PV` (Anomalia):**
    *   **Detalhes da Anomalia:** É **estritamente obrigatório** preencher os campos de detalhamento térmicos à anomalia: *Recomendação*, *Grau de Perigo* e *Status*.
    *   **Evidências:** Passa a ser obrigatório incluir uma *Observação* (comentário) em texto e adicionar pelo menos *uma Foto*.
*   **Para as opções `AU`, `DM`, `PC` ou `DS`:**
    *   **Evidências:** É **obrigatório** inserir uma *Observação* e adicionar pelo menos *uma Foto*.

*Nota: Tentar enviar o checklist (`_validateResponsesBeforeSaving`) com a ausência destas exigências exibirá diálogos de erro específicos para Anomalias que não foram detalhadas e para opções de estado (`AU, DM, PC, DS`) sem foto/observação.*

