# Contrato da API REST — Sistema de Chamados Técnicos

> Este documento representa o contrato inicial da camada REST do DEV 4. Campos, tipos, enums, assinaturas de services e exceptions de negócio serão consolidados após as entregas dos DEVs 2 e 3.

## Responsável

DEV 4 — API REST

## Escopo do MVP

A API REST deverá permitir:

- cadastrar usuários;
- cadastrar categorias;
- abrir chamados;
- consultar chamados;
- listar chamados com filtros e paginação;
- atribuir técnico;
- alterar status;
- adicionar comentários;
- consultar histórico de status.

Estão fora deste MVP:

- DELETE de chamado;
- frontend;
- anexos;
- JWT;
- e-mail;
- chat;
- aplicativo móvel.

## Endpoints

| Método | Endpoint | Objetivo | Resposta esperada |
| --- | --- | --- | --- |
| POST | /api/usuarios | Cadastrar usuário | 201 ou 422 |
| POST | /api/categorias | Cadastrar categoria | 201 ou 422 |
| POST | /api/chamados | Abrir chamado | 201 Created + Location |
| GET | /api/chamados | Listar e filtrar chamados | 200 + paginação |
| GET | /api/chamados/{id} | Consultar chamado | 200 ou 404 |
| PUT | /api/chamados/{id}/atribuir | Atribuir técnico | 200 |
| PATCH | /api/chamados/{id}/status | Alterar status | 200 ou 409 |
| POST | /api/chamados/{id}/comentarios | Adicionar comentário | 201 |
| GET | /api/chamados/{id}/historico | Consultar histórico | 200 |

## Filtros e paginação

Exemplos previstos no planejamento:

```http
GET /api/chamados?status=ABERTO&prioridade=ALTA&page=0&size=20
GET /api/chamados?tecnicoId=12&categoriaId=3
```

Parâmetros previstos:

- status;
- prioridade;
- tecnicoId;
- categoriaId;
- page;
- size.

Os valores dos exemplos são ilustrativos: não confirmam valores de enums, tipos de IDs ou valores padrão de paginação. Os tipos Java e os valores reais dos enums dependem da confirmação do DEV 2 e não são definidos neste documento.

## Regras da camada REST

- Controllers não devem receber entidades JPA diretamente.
- Controllers não devem devolver entidades JPA diretamente.
- Entrada e saída devem utilizar DTOs.
- Bean Validation deverá ser usada para validar requests com `@Valid`.
- Controllers não devem duplicar regras de negócio.
- Regras de negócio pertencem ao domínio/service.
- Erros devem ser tratados centralmente.
- A API deve usar `@RestControllerAdvice`.
- Erros devem ser retornados em JSON.

### Códigos de erro previstos

Esta classificação é um **contrato inicial sujeito à confirmação com DEV 3 para as exceptions reais**.

| Código HTTP | Significado previsto |
| --- | --- |
| 400 | Requisição inválida, JSON inválido ou parâmetros inválidos. |
| 404 | Recurso não encontrado. |
| 409 | Conflito de estado ou transição inválida. |
| 422 | Violação de regra de negócio ou validação semântica. |

## Dependências do DEV 4

### DEV 2 — necessário

- Usuario
- Categoria
- Chamado
- Comentario
- HistoricoStatus
- StatusChamado
- Prioridade
- Perfil
- repositories

### DEV 3 — necessário

- assinatura pública de ChamadoService;
- cadastro de usuário;
- cadastro de categoria;
- abertura de chamado;
- busca;
- listagem;
- atribuição;
- alteração de status;
- comentários;
- histórico;
- exceptions de negócio.

**Controllers e DTOs definitivos não devem ser implementados antes da confirmação desses contratos.**

## Decisões pendentes

- [ ] Campos definitivos de CriarUsuarioRequest
- [ ] Campos definitivos de CriarCategoriaRequest
- [ ] Campos definitivos de AbrirChamadoRequest
- [ ] Estrutura de ChamadoResponse
- [ ] Estrutura de ComentarioResponse
- [ ] Estrutura de HistoricoStatusResponse
- [ ] Tipo dos IDs
- [ ] Valores reais dos enums
- [ ] Assinaturas reais dos services
- [ ] Exceptions reais do domínio
- [ ] Formato final do JSON de erro
- [ ] Estrutura da resposta paginada
- [ ] Ordenação padrão da listagem
- [ ] Limite máximo de size

## Critérios de aceite

- Nenhuma entidade JPA exposta diretamente.
- Requests validados com Bean Validation.
- POST de chamado retorna 201 e Location.
- Busca inexistente retorna 404.
- Transição inválida retorna 409.
- Violação de regra de negócio retorna 422.
- JSON inválido retorna 400.
- Listagem suporta paginação.
- Listagem suporta filtros acordados.
- Erros seguem formato JSON único.
- Controllers delegam regras aos services.
- Testes MockMvc cobrem 201, 400, 404, 409 e 422.
- Build passa com Maven Wrapper.
