# 💊 Sistema de Gestão de Farmácia & Cosméticos

Sistema completo para farmácias e perfumarias desenvolvido em **Java** e com interface gráfica moderna construída em **HTML5 e CSS3**. 

O sistema possui controle de estoque com **códigos contados sequencialmente**, cadastro de clientes, equipe com instâncias da **Atendente** e **Farmacêutica**, **Janela de Venda** com **Função de Pagamento** (com taxa de 2% para crédito), persistência em **arquivos `.txt`** e **Relatório Geral de Vendas**.

---

## 🚀 Funcionalidades Principais

1. **Gestão de Cosméticos (Estoque)**:
   - Cadastramento de produtos cosméticos com **código gerado e contado automaticamente** (`#1`, `#2`, `#3`...).
   - Nome, marca, categoria, preço unitário e quantidade em estoque.
   - Consulta instantânea por código do produto com validação de disponibilidade.
   - Indicadores de estoque com alerta de estoque baixo.

2. **Equipe de Funcionários**:
   - Classe `Funcionario` contendo os objetos instanciados:
     - **Atendente**: `Camila Santos` (Registro: `MAT-1042`).
     - **Farmacêutica**: `Dra. Juliana Lima` (Registro: `CRF-SP 48.912`).

3. **Cadastro de Clientes**:
   - Registro de clientes com ID automático, Nome, CPF e Telefone.

4. **🛒 Janela de Venda (Balcão de Atendimento)**:
   - Seleção do cosmético a ser vendido e cálculo do subtotal em tempo real.
   - Validação imediata de estoque (impede vender mais do que está disponível).
   - Baixa automática no estoque ao concluir a venda.
   - Emissão de comprovante de venda na tela.

5. **💵 Função Pagamento com Cálculo de Taxa**:
   - **Dinheiro**: Sem taxa.
   - **Pix**: Sem taxa.
   - **Cartão de Débito**: Sem taxa.
   - **Cartão de Crédito**: **+2% de taxa** calculada e somada automaticamente ao total.

6. **📊 Relatório Geral de Vendas**:
   - Painel com KPIs: Faturamento Total, Quantidade de Transações, Unidades Vendidas e Total de Taxas Coletadas.
   - Resumo consolidado por forma de pagamento e por funcionário.
   - Histórico completo de transações detalhado.

7. **💾 Persistência em Arquivos TXT**:
   - `dados_farmacia.txt`: Armazena todos os produtos, clientes, funcionários e histórico de vendas.
   - `relatorio_vendas.txt`: Salva o relatório consolidado pronto para impressão ou consulta externa.

---

## 📁 Estrutura do Projeto

```text
Farmacia/
├── Main.java               # Código-fonte Java completo ("tudo em um único arquivo")
├── index.html              # Interface gráfica visual moderna em HTML5 e CSS3
├── dados_farmacia.txt      # Base de dados em formato texto persistente
├── relatorio_vendas.txt    # Relatório de vendas gerado automaticamente
├── .gitignore              # Ignora arquivos compilados (.class)
└── README.md               # Documentação do projeto
```

---

## 🛠️ Como Executar o Projeto

### Opção 1: Abrir diretamente no Navegador
1. Dê um duplo clique no arquivo `index.html`.
2. A interface abrirá no seu navegador (Google Chrome, Microsoft Edge, etc.) pronta para uso com todas as funções interativas.

### Opção 2: Rodar o Servidor Java
1. Abra o terminal (Prompt de Comando ou PowerShell) na pasta do projeto.
2. Compile e execute o arquivo `Main.java`:
   ```bash
   javac -encoding UTF-8 Main.java
   java Main
   ```
3. O servidor embutido iniciará em `http://localhost:8080`, abrindo automaticamente no seu navegador.

---

## 👥 Equipe do Sistema
- **Atendente**: Camila Santos
- **Farmacêutica**: Dra. Juliana Lima

---

## 📄 Licença
Este projeto foi desenvolvido para fins educacionais e práticos de desenvolvimento de software.
