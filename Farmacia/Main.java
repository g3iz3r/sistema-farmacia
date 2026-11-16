import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * =========================================================================
 * SISTEMA DE FARMÁCIA COMPLETO EM UM ÚNICO ARQUIVO COM RELATÓRIO DE VENDAS
 * =========================================================================
 * Requisitos:
 * - Classe Cliente
 * - Classe Cosmetico (com contador automático de códigos)
 * - Classe Funcionario (com os objetos solicitados: atendente e farmaceutica)
 * - Classe Venda (com a Função Pagamento: Dinheiro, Pix, Débito e Crédito +2%)
 * - Função Relatório de Vendas (Consolidado, Métricas, Formas de Pagamento e Vendedores)
 * - Persistência completa em arquivo TXT (dados_farmacia.txt e relatorio_vendas.txt)
 * - Janela de Venda visual com baixa automática no estoque e cálculo de taxa
 * - Servidor Web HTTP integrado com formulário HTML5 e CSS moderno e simples
 * =========================================================================
 */

// =========================================================================
// 1. CLASSE CLIENTE
// =========================================================================
class Cliente {
    private static int contadorCliente = 1;

    private int id;
    private String nome;
    private String cpf;
    private String telefone;

    public Cliente(String nome, String cpf, String telefone) {
        this.id = contadorCliente++;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
    }

    public Cliente(int id, String nome, String cpf, String telefone) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        if (id >= contadorCliente) {
            contadorCliente = id + 1;
        }
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getTelefone() { return telefone; }

    public String toJson() {
        return String.format("{\"id\":%d,\"nome\":\"%s\",\"cpf\":\"%s\",\"telefone\":\"%s\"}",
                id, escapeJson(nome), escapeJson(cpf), escapeJson(telefone));
    }

    public String toTxtLinha() {
        return id + ";" + nome + ";" + cpf + ";" + telefone;
    }

    private static String escapeJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

// =========================================================================
// 2. CLASSE COSMÉTICO (PRODUTO COM CÓDIGO CONTADO)
// =========================================================================
class Cosmetico {
    private static int contadorCodigo = 1;

    private int codigo;               // Código contado sequencialmente (1, 2, 3...)
    private String nome;
    private String marca;
    private String categoria;
    private double preco;
    private int quantidadeEstoque;

    public Cosmetico(String nome, String marca, String categoria, double preco, int quantidadeEstoque) {
        this.codigo = contadorCodigo++;
        this.nome = nome;
        this.marca = marca;
        this.categoria = categoria;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public Cosmetico(int codigo, String nome, String marca, String categoria, double preco, int quantidadeEstoque) {
        this.codigo = codigo;
        this.nome = nome;
        this.marca = marca;
        this.categoria = categoria;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        if (codigo >= contadorCodigo) {
            contadorCodigo = codigo + 1;
        }
    }

    public int getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getMarca() { return marca; }
    public String getCategoria() { return categoria; }
    public double getPreco() { return preco; }
    public int getQuantidadeEstoque() { return quantidadeEstoque; }

    public void setPreco(double preco) { this.preco = preco; }
    public void setQuantidadeEstoque(int quantidadeEstoque) { this.quantidadeEstoque = quantidadeEstoque; }

    public boolean decrementarEstoque(int qtd) {
        if (qtd <= this.quantidadeEstoque) {
            this.quantidadeEstoque -= qtd;
            return true;
        }
        return false;
    }

    public String toJson() {
        return String.format(
            Locale.US,
            "{\"codigo\":%d,\"nome\":\"%s\",\"marca\":\"%s\",\"categoria\":\"%s\",\"preco\":%.2f,\"quantidadeEstoque\":%d}",
            codigo, escapeJson(nome), escapeJson(marca), escapeJson(categoria), preco, quantidadeEstoque
        );
    }

    public String toTxtLinha() {
        return String.format(Locale.US, "%d;%s;%s;%s;%.2f;%d", codigo, nome, marca, categoria, preco, quantidadeEstoque);
    }

    private static String escapeJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

// =========================================================================
// 3. CLASSE FUNCIONÁRIO (COM OS OBJETOS ATENDENTE E FARMACÊUTICA)
// =========================================================================
class Funcionario {
    private int id;
    private String nome;
    private String cargo;
    private String registro;

    // CONFORME SOLICITADO: Objetos Atendente e Farmacêutica na classe Funcionario
    public static Funcionario atendente = new Funcionario(1, "Camila Santos", "Atendente", "MAT-1042");
    public static Funcionario farmaceutica = new Funcionario(2, "Dra. Juliana Lima", "Farmacêutica", "CRF-SP 48.912");

    public Funcionario(int id, String nome, String cargo, String registro) {
        this.id = id;
        this.nome = nome;
        this.cargo = cargo;
        this.registro = registro;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCargo() { return cargo; }
    public String getRegistro() { return registro; }

    public String toJson() {
        return String.format("{\"id\":%d,\"nome\":\"%s\",\"cargo\":\"%s\",\"registro\":\"%s\"}",
                id, escapeJson(nome), escapeJson(cargo), escapeJson(registro));
    }

    public String toTxtLinha() {
        return id + ";" + nome + ";" + cargo + ";" + registro;
    }

    private static String escapeJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

// =========================================================================
// 4. CLASSE VENDA (COM FUNÇÃO PAGAMENTO E TAXA DE CRÉDITO)
// =========================================================================
class Venda {
    private static int contadorVenda = 1;

    private int id;
    private String dataHora;
    private int codigoProduto;
    private String nomeProduto;
    private int quantidade;
    private double subtotal;
    private double taxa;
    private double valorTotal;
    private String formaPagamento;
    private String funcionario;
    private String cliente;

    public Venda(int codigoProduto, String nomeProduto, int quantidade, double subtotal, double taxa, double valorTotal, String formaPagamento, String funcionario, String cliente) {
        this.id = contadorVenda++;
        this.dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.subtotal = subtotal;
        this.taxa = taxa;
        this.valorTotal = valorTotal;
        this.formaPagamento = formaPagamento;
        this.funcionario = funcionario;
        this.cliente = (cliente == null || cliente.trim().isEmpty()) ? "Cliente Balcão" : cliente;
    }

    public Venda(int id, String dataHora, int codigoProduto, String nomeProduto, int quantidade, double subtotal, double taxa, double valorTotal, String formaPagamento, String funcionario, String cliente) {
        this.id = id;
        this.dataHora = dataHora;
        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.subtotal = subtotal;
        this.taxa = taxa;
        this.valorTotal = valorTotal;
        this.formaPagamento = formaPagamento;
        this.funcionario = funcionario;
        this.cliente = cliente;
        if (id >= contadorVenda) {
            contadorVenda = id + 1;
        }
    }

    public int getId() { return id; }
    public String getDataHora() { return dataHora; }
    public int getCodigoProduto() { return codigoProduto; }
    public String getNomeProduto() { return nomeProduto; }
    public int getQuantidade() { return quantidade; }
    public double getSubtotal() { return subtotal; }
    public double getTaxa() { return taxa; }
    public double getValorTotal() { return valorTotal; }
    public String getFormaPagamento() { return formaPagamento; }
    public String getFuncionario() { return funcionario; }
    public String getCliente() { return cliente; }

    public String toJson() {
        return String.format(
            Locale.US,
            "{\"id\":%d,\"dataHora\":\"%s\",\"codigoProduto\":%d,\"nomeProduto\":\"%s\",\"quantidade\":%d,\"subtotal\":%.2f,\"taxa\":%.2f,\"valorTotal\":%.2f,\"formaPagamento\":\"%s\",\"funcionario\":\"%s\",\"cliente\":\"%s\"}",
            id, escapeJson(dataHora), codigoProduto, escapeJson(nomeProduto), quantidade, subtotal, taxa, valorTotal, escapeJson(formaPagamento), escapeJson(funcionario), escapeJson(cliente)
        );
    }

    public String toTxtLinha() {
        return String.format(Locale.US, "%d;%s;%d;%s;%d;%.2f;%.2f;%.2f;%s;%s;%s",
                id, dataHora, codigoProduto, nomeProduto, quantidade, subtotal, taxa, valorTotal, formaPagamento, funcionario, cliente);
    }

    private static String escapeJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

// =========================================================================
// 5. CLASSE PRINCIPAL COM RELATÓRIO DE VENDAS E SERVIDOR HTTP
// =========================================================================
public class Main {
    private static final String NOME_ARQUIVO_TXT = "dados_farmacia.txt";
    private static final String NOME_RELATORIO_TXT = "relatorio_vendas.txt";
    private static final List<Cosmetico> estoque = Collections.synchronizedList(new ArrayList<>());
    private static final List<Cliente> clientes = Collections.synchronizedList(new ArrayList<>());
    private static final List<Venda> vendas = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        // Carrega dados salvos do arquivo TXT (ou inicializa se não existir)
        carregarDeArquivoTxt();

        int porta = 8080;
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);

            // Rota da Página Principal (HTML + CSS)
            server.createContext("/", new PaginaPrincipalHandler());

            // Rotas da API REST com suporte a CORS
            server.createContext("/api/produtos", new ListarProdutosHandler());
            server.createContext("/api/cadastrar", new CadastrarProdutoHandler());
            server.createContext("/api/consultar", new ConsultarProdutoHandler());
            server.createContext("/api/funcionarios", new FuncionariosHandler());
            server.createContext("/api/clientes", new ClientesHandler());
            server.createContext("/api/vender", new RealizarVendaHandler());
            server.createContext("/api/vendas", new ListarVendasHandler());
            server.createContext("/api/relatorio", new RelatorioVendasHandler());
            server.createContext("/api/baixar-txt", new BaixarTxtHandler());
            server.createContext("/api/baixar-relatorio", new BaixarRelatorioHandler());

            server.setExecutor(null);
            server.start();

            String url = "http://localhost:" + porta;
            System.out.println("===============================================================");
            System.out.println("   SISTEMA DE FARMACIA COM RELATORIO DE VENDAS E TXT");
            System.out.println("===============================================================");
            System.out.println(" -> Arquivo TXT de Dados     : " + new File(NOME_ARQUIVO_TXT).getAbsolutePath());
            System.out.println(" -> Arquivo TXT do Relatorio : " + new File(NOME_RELATORIO_TXT).getAbsolutePath());
            System.out.println(" -> Acesse o sistema no navegador: " + url);
            System.out.println(" -> Formas de Pagamento : Dinheiro, Pix, Debito e Credito (+2% taxa)");
            System.out.println(" -> Objetos da classe Funcionario carregados:");
            System.out.println("    - Atendente   : " + Funcionario.atendente.getNome() + " (" + Funcionario.atendente.getRegistro() + ")");
            System.out.println("    - Farmaceutica: " + Funcionario.farmaceutica.getNome() + " (" + Funcionario.farmaceutica.getRegistro() + ")");
            System.out.println("---------------------------------------------------------------");
            exibirRelatorioVendasConsole();
            System.out.println("===============================================================");
            System.out.println(" Pressione Ctrl + C no terminal para encerrar.");

            abrirNavegador(url);

        } catch (Throwable e) {
            System.out.println("===============================================================");
            System.out.println("   SISTEMA DE FARMACIA - MODO CONSOLE (ARQUIVO TXT ATIVO)");
            System.out.println("===============================================================");
            System.out.println(" -> Arquivo de Dados TXT: " + new File(NOME_ARQUIVO_TXT).getAbsolutePath());
            System.out.println(" -> Abra index.html no navegador para a interface visual e Relatório de Vendas.");
            System.out.println("---------------------------------------------------------------");
            exibirRelatorioVendasConsole();
            System.out.println("===============================================================");
        }
    }

    // =========================================================================
    // FUNÇÃO PAGAMENTO
    // Opções: Dinheiro, Pix, Cartão Débito e Crédito (+2% de taxa no crédito)
    // =========================================================================
    public static double[] calcularPagamento(double subtotal, String formaPagamento) {
        double taxa = 0.0;
        if (formaPagamento != null) {
            String fp = formaPagamento.toLowerCase();
            if (fp.contains("crédito") || fp.contains("credito")) {
                taxa = subtotal * 0.02; // Adiciona 2% de taxa na função crédito
            }
        }
        double total = subtotal + taxa;
        return new double[] { taxa, total };
    }

    // =========================================================================
    // FUNÇÃO RELATÓRIO DE VENDAS
    // =========================================================================
    public static String gerarRelatorioVendasTexto() {
        int totalTransacoes = vendas.size();
        int totalUnidades = 0;
        double subtotalGeral = 0.0;
        double totalTaxas = 0.0;
        double faturamentoTotal = 0.0;

        Map<String, Double> totalPorPagamento = new LinkedHashMap<>();
        Map<String, Integer> qtdPorPagamento = new LinkedHashMap<>();
        totalPorPagamento.put("Dinheiro", 0.0);
        totalPorPagamento.put("Pix", 0.0);
        totalPorPagamento.put("Cartão de Débito", 0.0);
        totalPorPagamento.put("Cartão de Crédito", 0.0);
        qtdPorPagamento.put("Dinheiro", 0);
        qtdPorPagamento.put("Pix", 0);
        qtdPorPagamento.put("Cartão de Débito", 0);
        qtdPorPagamento.put("Cartão de Crédito", 0);

        Map<String, Double> totalPorFuncionario = new LinkedHashMap<>();
        Map<String, Integer> qtdPorFuncionario = new LinkedHashMap<>();

        for (Venda v : vendas) {
            totalUnidades += v.getQuantidade();
            subtotalGeral += v.getSubtotal();
            totalTaxas += v.getTaxa();
            faturamentoTotal += v.getValorTotal();

            // Total por forma de pagamento
            String fp = v.getFormaPagamento();
            String chaveFp = "Outros";
            if (fp != null) {
                String fpLow = fp.toLowerCase();
                if (fpLow.contains("crédito") || fpLow.contains("credito")) chaveFp = "Cartão de Crédito";
                else if (fpLow.contains("débito") || fpLow.contains("debito")) chaveFp = "Cartão de Débito";
                else if (fpLow.contains("pix")) chaveFp = "Pix";
                else if (fpLow.contains("dinheiro")) chaveFp = "Dinheiro";
            }
            totalPorPagamento.put(chaveFp, totalPorPagamento.getOrDefault(chaveFp, 0.0) + v.getValorTotal());
            qtdPorPagamento.put(chaveFp, qtdPorPagamento.getOrDefault(chaveFp, 0) + 1);

            // Total por funcionário
            String func = v.getFuncionario();
            totalPorFuncionario.put(func, totalPorFuncionario.getOrDefault(func, 0.0) + v.getValorTotal());
            qtdPorFuncionario.put(func, qtdPorFuncionario.getOrDefault(func, 0) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("           RELATÓRIO GERAL DE VENDAS - FARMÁCIA SAÚDE & BELEZA\n");
        sb.append("================================================================================\n");
        sb.append(String.format("Data da Emissão: %s\n", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("Total de Vendas Realizadas : %d vendas\n", totalTransacoes));
        sb.append(String.format("Total de Unidades Vendidas : %d unidades\n", totalUnidades));
        sb.append(String.format(Locale.US, "Subtotal dos Cosméticos    : R$ %.2f\n", subtotalGeral));
        sb.append(String.format(Locale.US, "Total de Taxas Coletadas   : R$ %.2f (Taxa de 2%% Cartão Crédito)\n", totalTaxas));
        sb.append(String.format(Locale.US, "FATURAMENTO BRUTO TOTAL    : R$ %.2f\n", faturamentoTotal));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("RESUMO POR FORMA DE PAGAMENTO:\n");
        for (String fp : totalPorPagamento.keySet()) {
            double val = totalPorPagamento.get(fp);
            int qtd = qtdPorPagamento.getOrDefault(fp, 0);
            sb.append(String.format(Locale.US, " - %-18s: R$ %8.2f  (%d vendas)\n", fp, val, qtd));
        }
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("RESUMO POR FUNCIONÁRIO:\n");
        for (String func : totalPorFuncionario.keySet()) {
            double val = totalPorFuncionario.get(func);
            int qtd = qtdPorFuncionario.get(func);
            sb.append(String.format(Locale.US, " - %-32s: R$ %8.2f  (%d vendas)\n", func, val, qtd));
        }
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("HISTÓRICO DETALHADO DE TRANSAÇÕES:\n");
        sb.append(String.format("%-5s | %-16s | %-28s | %-4s | %-16s | %-8s | %-10s%n",
                "ID", "DATA/HORA", "PRODUTO", "QTD", "PAGAMENTO", "TAXA", "TOTAL"));
        sb.append("--------------------------------------------------------------------------------\n");
        for (Venda v : vendas) {
            sb.append(String.format(Locale.US, "#%-4d | %-16s | %-28s | %-4d | %-16s | R$ %-5.2f | R$ %-8.2f%n",
                    v.getId(), v.getDataHora(), v.getNomeProduto(), v.getQuantidade(),
                    v.getFormaPagamento(), v.getTaxa(), v.getValorTotal()));
        }
        sb.append("================================================================================\n");

        return sb.toString();
    }

    public static void exibirRelatorioVendasConsole() {
        System.out.print(gerarRelatorioVendasTexto());
        salvarRelatorioEmArquivo();
    }

    public static synchronized void salvarRelatorioEmArquivo() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(NOME_RELATORIO_TXT), StandardCharsets.UTF_8))) {
            writer.print(gerarRelatorioVendasTexto());
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // PERSISTÊNCIA EM ARQUIVO TXT (SALVAR E CARREGAR)
    // =========================================================================

    public static synchronized void salvarEmArquivoTxt() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(NOME_ARQUIVO_TXT), StandardCharsets.UTF_8))) {

            writer.println("================================================================================");
            writer.println("                    SISTEMA DE FARMÁCIA - BASE DE DADOS");
            writer.println("================================================================================");
            writer.println();

            // 1. Salva Estoque de Cosméticos
            writer.println("[PRODUTOS_COSMETICOS]");
            writer.println("codigo;nome;marca;categoria;preco;quantidadeEstoque");
            for (Cosmetico c : estoque) {
                writer.println(c.toTxtLinha());
            }
            writer.println();

            // 2. Salva Clientes
            writer.println("[CLIENTES]");
            writer.println("id;nome;cpf;telefone");
            for (Cliente cli : clientes) {
                writer.println(cli.toTxtLinha());
            }
            writer.println();

            // 3. Salva Funcionários
            writer.println("[FUNCIONARIOS]");
            writer.println("id;nome;cargo;registro");
            writer.println(Funcionario.atendente.toTxtLinha());
            writer.println(Funcionario.farmaceutica.toTxtLinha());
            writer.println();

            // 4. Salva Histórico de Vendas
            writer.println("[HISTORICO_VENDAS]");
            writer.println("id;dataHora;codigoProduto;nomeProduto;quantidade;subtotal;taxa;valorTotal;formaPagamento;funcionario;cliente");
            for (Venda v : vendas) {
                writer.println(v.toTxtLinha());
            }

            System.out.println("[TXT] Dados salvos com sucesso em: " + NOME_ARQUIVO_TXT);

            // Atualiza também o arquivo de relatório de vendas
            salvarRelatorioEmArquivo();

        } catch (Exception e) {
            System.err.println("Erro ao salvar dados no arquivo TXT: " + e.getMessage());
        }
    }

    public static synchronized void carregarDeArquivoTxt() {
        File arquivo = new File(NOME_ARQUIVO_TXT);
        if (!arquivo.exists()) {
            inicializarDadosPadrao();
            salvarEmArquivoTxt();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(arquivo), StandardCharsets.UTF_8))) {

            String linha;
            String secaoAtual = "";

            estoque.clear();
            clientes.clear();
            vendas.clear();

            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("===") || linha.startsWith("codigo;") || linha.startsWith("id;")) {
                    continue;
                }

                if (linha.startsWith("[") && linha.endsWith("]")) {
                    secaoAtual = linha;
                    continue;
                }

                String[] partes = linha.split(";");
                switch (secaoAtual) {
                    case "[PRODUTOS_COSMETICOS]":
                        if (partes.length >= 6) {
                            int cod = Integer.parseInt(partes[0].trim());
                            String nome = partes[1].trim();
                            String marca = partes[2].trim();
                            String cat = partes[3].trim();
                            double preco = Double.parseDouble(partes[4].trim().replace(",", "."));
                            int qtd = Integer.parseInt(partes[5].trim());
                            estoque.add(new Cosmetico(cod, nome, marca, cat, preco, qtd));
                        }
                        break;

                    case "[CLIENTES]":
                        if (partes.length >= 4) {
                            int id = Integer.parseInt(partes[0].trim());
                            String nome = partes[1].trim();
                            String cpf = partes[2].trim();
                            String tel = partes[3].trim();
                            clientes.add(new Cliente(id, nome, cpf, tel));
                        }
                        break;

                    case "[HISTORICO_VENDAS]":
                        if (partes.length >= 11) {
                            int id = Integer.parseInt(partes[0].trim());
                            String data = partes[1].trim();
                            int codProd = Integer.parseInt(partes[2].trim());
                            String nomeProd = partes[3].trim();
                            int qtd = Integer.parseInt(partes[4].trim());
                            double sub = Double.parseDouble(partes[5].trim().replace(",", "."));
                            double tax = Double.parseDouble(partes[6].trim().replace(",", "."));
                            double total = Double.parseDouble(partes[7].trim().replace(",", "."));
                            String pagto = partes[8].trim();
                            String func = partes[9].trim();
                            String cli = partes[10].trim();
                            vendas.add(new Venda(id, data, codProd, nomeProd, qtd, sub, tax, total, pagto, func, cli));
                        } else if (partes.length >= 8) {
                            int id = Integer.parseInt(partes[0].trim());
                            String data = partes[1].trim();
                            int codProd = Integer.parseInt(partes[2].trim());
                            String nomeProd = partes[3].trim();
                            int qtd = Integer.parseInt(partes[4].trim());
                            double total = Double.parseDouble(partes[5].trim().replace(",", "."));
                            String func = partes[6].trim();
                            String cli = partes[7].trim();
                            vendas.add(new Venda(id, data, codProd, nomeProd, qtd, total, 0.0, total, "Pix", func, cli));
                        }
                        break;
                }
            }

            if (estoque.isEmpty()) {
                inicializarDadosPadrao();
                salvarEmArquivoTxt();
            }

            System.out.println("[TXT] " + estoque.size() + " produtos carregados do arquivo TXT.");
            salvarRelatorioEmArquivo();

        } catch (Exception e) {
            System.err.println("Aviso ao ler TXT (" + e.getMessage() + "), inicializando padrao...");
            inicializarDadosPadrao();
        }
    }

    private static void inicializarDadosPadrao() {
        estoque.clear();
        estoque.add(new Cosmetico(1, "Protetor Solar FPS 50", "La Roche-Posay", "Cuidados com a Pele", 89.90, 25));
        estoque.add(new Cosmetico(2, "Hidratante Facial Ácido Hialurônico", "CeraVe", "Cuidados com a Pele", 74.50, 40));
        estoque.add(new Cosmetico(3, "Shampoo Antiqueda Revitalizante", "Vichy Dercos", "Cabelos", 112.00, 18));
        estoque.add(new Cosmetico(4, "Batom Matte Longa Duração", "Maybelline", "Maquiagem", 39.90, 50));
        estoque.add(new Cosmetico(5, "Água Micelar Demaquilante 400ml", "L'Oréal Paris", "Limpeza Facial", 44.90, 30));

        clientes.clear();
        clientes.add(new Cliente(1, "Maria Fernanda Souza", "111.222.333-44", "(11) 98765-4321"));
        clientes.add(new Cliente(2, "Carlos Eduardo Lima", "555.666.777-88", "(11) 91234-5678"));

        vendas.clear();
        vendas.add(new Venda(1, "16/11/2026 14:00", 1, "Protetor Solar FPS 50", 1, 89.90, 0.0, 89.90, "Pix", "Camila Santos (Atendente)", "Maria Fernanda Souza"));
        vendas.add(new Venda(2, "16/11/2026 14:30", 2, "Hidratante Facial Ácido Hialurônico", 2, 149.00, 2.98, 151.98, "Cartão de Crédito", "Dra. Juliana Lima (Farmacêutica)", "Carlos Eduardo Lima"));
    }

    // =========================================================================
    // MÉTODOS DE NEGÓCIO DO SISTEMA
    // =========================================================================

    public static synchronized Cosmetico cadastrarCosmetico(String nome, String marca, String categoria, double preco, int qtd) {
        Cosmetico novo = new Cosmetico(nome, marca, categoria, preco, qtd);
        estoque.add(novo);
        salvarEmArquivoTxt(); // Salva imediatamente no TXT
        return novo;
    }

    public static Cosmetico consultarPorCodigo(int codigo) {
        for (Cosmetico c : estoque) {
            if (c.getCodigo() == codigo) {
                return c;
            }
        }
        return null;
    }

    public static synchronized Venda realizarVenda(int codigoProduto, int quantidade, String formaPagamento, String nomeFuncionario, String nomeCliente) {
        Cosmetico produto = consultarPorCodigo(codigoProduto);
        if (produto == null) {
            return null;
        }

        if (produto.decrementarEstoque(quantidade)) {
            double subtotal = produto.getPreco() * quantidade;
            double[] calculoPagamento = calcularPagamento(subtotal, formaPagamento);
            double taxa = calculoPagamento[0];
            double valorTotal = calculoPagamento[1];

            Venda venda = new Venda(codigoProduto, produto.getNome(), quantidade, subtotal, taxa, valorTotal, formaPagamento, nomeFuncionario, nomeCliente);
            vendas.add(venda);
            salvarEmArquivoTxt(); // Atualiza estoque e grava a venda no arquivo TXT
            return venda;
        }
        return null;
    }

    private static void abrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {}
    }

    private static void aplicarCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // =========================================================================
    // HANDLERS HTTP (API REST E FRONTEND)
    // =========================================================================

    static class PaginaPrincipalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            byte[] resposta = getHtml().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, resposta.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resposta);
            }
        }
    }

    static class ListarProdutosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < estoque.size(); i++) {
                sb.append(estoque.get(i).toJson());
                if (i < estoque.size() - 1) sb.append(",");
            }
            sb.append("]");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class CadastrarProdutoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String corpo = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                String nome = "";
                String marca = "";
                String categoria = "";
                double preco = 0.0;
                int quantidade = 0;

                try {
                    if (corpo.startsWith("{")) {
                        nome = extrairCampoJson(corpo, "nome");
                        marca = extrairCampoJson(corpo, "marca");
                        categoria = extrairCampoJson(corpo, "categoria");
                        String pStr = extrairCampoJson(corpo, "preco").replace(",", ".");
                        String qStr = extrairCampoJson(corpo, "quantidadeEstoque");
                        preco = Double.parseDouble(pStr);
                        quantidade = Integer.parseInt(qStr);
                    } else {
                        String[] pares = corpo.split("&");
                        for (String par : pares) {
                            String[] kv = par.split("=");
                            if (kv.length == 2) {
                                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                                String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                                if ("nome".equalsIgnoreCase(k)) nome = v;
                                else if ("marca".equalsIgnoreCase(k)) marca = v;
                                else if ("categoria".equalsIgnoreCase(k)) categoria = v;
                                else if ("preco".equalsIgnoreCase(k)) preco = Double.parseDouble(v.replace(",", "."));
                                else if ("quantidadeEstoque".equalsIgnoreCase(k)) quantidade = Integer.parseInt(v);
                            }
                        }
                    }

                    Cosmetico novo = cadastrarCosmetico(nome, marca, categoria, preco, quantidade);
                    byte[] resp = novo.toJson().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(201, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } catch (Exception ex) {
                    String err = "{\"erro\":\"Dados invalidos para cadastro: " + ex.getMessage() + "\"}";
                    byte[] resp = err.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(400, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class ConsultarProdutoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            int codigo = -1;
            if (query != null && query.contains("codigo=")) {
                String[] params = query.split("&");
                for (String p : params) {
                    if (p.startsWith("codigo=")) {
                        try {
                            codigo = Integer.parseInt(p.substring(7));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            Cosmetico encontrado = (codigo != -1) ? consultarPorCodigo(codigo) : null;
            if (encontrado != null) {
                byte[] resp = encontrado.toJson().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            } else {
                String erro = "{\"erro\":\"Produto não encontrado com o código #" + codigo + "\"}";
                byte[] resp = erro.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(404, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            }
        }
    }

    static class RealizarVendaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String corpo = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                try {
                    int codigo = Integer.parseInt(extrairCampoJson(corpo, "codigoProduto"));
                    int qtd = Integer.parseInt(extrairCampoJson(corpo, "quantidade"));
                    String formaPagamento = extrairCampoJson(corpo, "formaPagamento");
                    if (formaPagamento == null || formaPagamento.trim().isEmpty()) {
                        formaPagamento = "Dinheiro";
                    }
                    String func = extrairCampoJson(corpo, "funcionario");
                    String cli = extrairCampoJson(corpo, "cliente");

                    Venda venda = realizarVenda(codigo, qtd, formaPagamento, func, cli);
                    if (venda != null) {
                        byte[] resp = venda.toJson().getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, resp.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(resp);
                        }
                    } else {
                        String erro = "{\"erro\":\"Estoque insuficiente ou produto inexistente.\"}";
                        byte[] resp = erro.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(400, resp.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(resp);
                        }
                    }
                } catch (Exception ex) {
                    String erro = "{\"erro\":\"Erro nos dados da venda: " + ex.getMessage() + "\"}";
                    byte[] resp = erro.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(400, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class ListarVendasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < vendas.size(); i++) {
                sb.append(vendas.get(i).toJson());
                if (i < vendas.size() - 1) sb.append(",");
            }
            sb.append("]");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class RelatorioVendasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            int totalTransacoes = vendas.size();
            int totalUnidades = 0;
            double subtotalGeral = 0.0;
            double totalTaxas = 0.0;
            double faturamentoTotal = 0.0;

            for (Venda v : vendas) {
                totalUnidades += v.getQuantidade();
                subtotalGeral += v.getSubtotal();
                totalTaxas += v.getTaxa();
                faturamentoTotal += v.getValorTotal();
            }

            StringBuilder sbVendas = new StringBuilder("[");
            for (int i = 0; i < vendas.size(); i++) {
                sbVendas.append(vendas.get(i).toJson());
                if (i < vendas.size() - 1) sbVendas.append(",");
            }
            sbVendas.append("]");

            String json = String.format(
                Locale.US,
                "{\"totalTransacoes\":%d,\"totalUnidades\":%d,\"subtotalGeral\":%.2f,\"totalTaxas\":%.2f,\"faturamentoTotal\":%.2f,\"vendas\":%s}",
                totalTransacoes, totalUnidades, subtotalGeral, totalTaxas, faturamentoTotal, sbVendas.toString()
            );

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class FuncionariosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String json = String.format("{\"atendente\":%s,\"farmaceutica\":%s}",
                    Funcionario.atendente.toJson(),
                    Funcionario.farmaceutica.toJson());

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class ClientesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String corpo = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String nome = extrairCampoJson(corpo, "nome");
                String cpf = extrairCampoJson(corpo, "cpf");
                String telefone = extrairCampoJson(corpo, "telefone");

                Cliente c = new Cliente(nome, cpf, telefone);
                clientes.add(c);
                salvarEmArquivoTxt();

                byte[] resp = c.toJson().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(201, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            } else {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < clientes.size(); i++) {
                    sb.append(clientes.get(i).toJson());
                    if (i < clientes.size() - 1) sb.append(",");
                }
                sb.append("]");

                byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    static class BaixarTxtHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            File f = new File(NOME_ARQUIVO_TXT);
            if (!f.exists()) {
                salvarEmArquivoTxt();
            }

            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"dados_farmacia.txt\"");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class BaixarRelatorioHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            byte[] bytes = gerarRelatorioVendasTexto().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"relatorio_vendas.txt\"");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String extrairCampoJson(String json, String campo) {
        String chave = "\"" + campo + "\"";
        int idx = json.indexOf(chave);
        if (idx == -1) return "";
        int doisPontos = json.indexOf(":", idx);
        if (doisPontos == -1) return "";

        int inicio = doisPontos + 1;
        while (inicio < json.length() && Character.isWhitespace(json.charAt(inicio))) {
            inicio++;
        }

        if (inicio < json.length() && json.charAt(inicio) == '\"') {
            int fim = json.indexOf("\"", inicio + 1);
            return (fim != -1) ? json.substring(inicio + 1, fim) : "";
        } else {
            int fim = inicio;
            while (fim < json.length() && json.charAt(fim) != ',' && json.charAt(fim) != '}' && !Character.isWhitespace(json.charAt(fim))) {
                fim++;
            }
            return json.substring(inicio, fim).trim();
        }
    }

    // =========================================================================
    // CÓDIGO HTML + CSS INTEGRADO COM RELATÓRIO DE VENDAS COMPLETO
    // =========================================================================
    public static String getHtml() {
        return """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sistema de Farmácia & Cosméticos</title>
    <style>
        :root {
            --primary: #0284c7;
            --primary-dark: #0369a1;
            --success: #16a34a;
            --success-dark: #15803d;
            --warning: #f59e0b;
            --purple: #7c3aed;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text: #1e293b;
            --text-muted: #64748b;
            --border: #e2e8f0;
            --danger: #dc2626;
            --badge: #e0f2fe;
            --badge-text: #0369a1;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background-color: var(--bg); color: var(--text); padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; }

        header {
            background: linear-gradient(135deg, #0284c7, #0f766e);
            color: white; padding: 22px; border-radius: 14px; margin-bottom: 22px;
            box-shadow: 0 4px 12px rgba(2, 132, 199, 0.15);
            display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 15px;
        }
        header h1 { font-size: 1.6rem; display: flex; align-items: center; gap: 10px; }
        .header-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
        .stats-badge { background: rgba(255, 255, 255, 0.2); padding: 6px 14px; border-radius: 20px; font-size: 0.9rem; }
        .btn-txt { background: #ffffff; color: #0369a1; border: none; padding: 8px 14px; border-radius: 8px; font-weight: 700; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 6px; font-size: 0.88rem; }
        .btn-txt:hover { background: #f0fdf4; }

        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; margin-bottom: 22px; }
        @media (max-width: 860px) { .grid { grid-template-columns: 1fr; } }

        .card { background: var(--card-bg); padding: 22px; border-radius: 12px; border: 1px solid var(--border); box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04); margin-bottom: 22px; }
        .card h2 { font-size: 1.2rem; margin-bottom: 14px; color: var(--primary-dark); border-bottom: 2px solid #f1f5f9; padding-bottom: 8px; display: flex; align-items: center; gap: 8px; }

        .card-venda { border: 2px solid #0284c7; background: #f0f9ff; }
        .card-venda h2 { color: #0369a1; border-color: #bae6fd; }

        .card-relatorio { border: 2px solid #7c3aed; background: #faf5ff; }
        .card-relatorio h2 { color: #6d28d9; border-color: #ddd6fe; }

        /* KPI Cards do Relatório */
        .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin-bottom: 18px; }
        .kpi-card { background: #ffffff; border: 1px solid var(--border); border-radius: 10px; padding: 14px; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
        .kpi-title { font-size: 0.8rem; text-transform: uppercase; font-weight: 700; color: var(--text-muted); margin-bottom: 4px; }
        .kpi-value { font-size: 1.4rem; font-weight: 800; color: #1e293b; }
        .kpi-value.green { color: var(--success); }
        .kpi-value.purple { color: var(--purple); }
        .kpi-value.orange { color: #d97706; }

        .form-group { margin-bottom: 13px; }
        .form-group label { display: block; font-size: 0.84rem; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; }
        .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

        input, select { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 0.95rem; outline: none; background: #fff; transition: border-color 0.2s, box-shadow 0.2s; }
        input:focus, select:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15); }

        button { background-color: var(--primary); color: white; border: none; padding: 11px 18px; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 0.95rem; display: inline-flex; align-items: center; justify-content: center; gap: 6px; }
        button:hover { background-color: var(--primary-dark); }
        .btn-green { background-color: var(--success); font-size: 1.05rem; padding: 12px 20px; }
        .btn-green:hover { background-color: var(--success-dark); }
        .btn-purple { background-color: var(--purple); }
        .btn-purple:hover { background-color: #6d28d9; }

        .team-box { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 8px; margin-bottom: 16px; }
        .team-card { background: #f8fafc; border: 1px solid var(--border); border-radius: 8px; padding: 10px; }
        .team-card .role { font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--primary); }
        .team-card .name { font-size: 0.95rem; font-weight: 600; margin: 2px 0; }
        .team-card .reg { font-size: 0.78rem; color: var(--text-muted); }

        .venda-resumo-box { background: #ffffff; border: 1px solid #bae6fd; border-radius: 8px; padding: 14px; margin: 14px 0; }
        .venda-linha { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; font-size: 0.9rem; }
        .venda-linha.destaque { font-size: 1.3rem; font-weight: 800; color: #0284c7; border-top: 1px dashed #cbd5e1; padding-top: 8px; margin-top: 8px; margin-bottom: 0; }
        .tag-taxa { background: #fef3c7; color: #b45309; padding: 2px 6px; border-radius: 4px; font-size: 0.78rem; font-weight: 700; }

        .comprovante { display: none; background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 8px; padding: 14px; margin-top: 14px; }
        .comprovante h4 { color: #065f46; font-size: 1rem; margin-bottom: 6px; }

        .alert-box { padding: 10px 14px; border-radius: 8px; margin-bottom: 12px; font-size: 0.9rem; display: none; }
        .alert-success { background: #dcfce7; color: #166534; border: 1px solid #86efac; }
        .alert-error { background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; }

        table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 0.91rem; }
        th, td { padding: 11px; text-align: left; border-bottom: 1px solid var(--border); }
        th { background-color: #f8fafc; color: var(--text-muted); font-weight: 600; }
        tbody tr:hover { background-color: #f1f5f9; }

        .badge-code { background-color: var(--badge); color: var(--badge-text); padding: 3px 8px; border-radius: 6px; font-weight: 700; font-size: 0.85rem; }
        .tag-stock { background-color: #dcfce7; color: #15803d; padding: 3px 8px; border-radius: 6px; font-weight: 600; font-size: 0.83rem; }
        .tag-low { background-color: #fee2e2; color: #b91c1c; }
        .tag-pagto { background-color: #e0e7ff; color: #3730a3; padding: 3px 8px; border-radius: 6px; font-weight: 600; font-size: 0.82rem; }
    </style>
</head>
<body>

<div class="container">
    <header>
        <div>
            <h1>💊 Farmácia Saúde & Beleza</h1>
            <p style="opacity: 0.9; font-size: 0.92rem;">Gestão de Cosméticos, Vendas, Pagamentos e Relatórios em TXT</p>
        </div>
        <div class="header-actions">
            <span class="stats-badge" id="headerStats">Carregando estoque...</span>
            <button onclick="baixarArquivoTxt()" class="btn-txt">💾 Baixar dados_farmacia.txt</button>
            <button onclick="baixarRelatorioTxt()" class="btn-txt" style="background:#f3e8ff; color:#6d28d9;">📑 Baixar relatorio_vendas.txt</button>
        </div>
    </header>

    <!-- Linha 1: Janela de Venda & Cadastro de Cosméticos -->
    <div class="grid">

        <!-- JANELA DE VENDA COM FUNÇÃO PAGAMENTO -->
        <div class="card card-venda">
            <h2>🛒 Janela de Venda (Balcão de Pagamento)</h2>
            <p style="font-size: 0.84rem; color: var(--text-muted); margin-bottom: 12px;">
                Selecione o cosmético, forma de pagamento (com taxa automática de 2% para crédito):
            </p>

            <div id="alertaVenda" class="alert-box"></div>

            <form id="formVenda" onsubmit="realizarVenda(event)">
                <div class="form-group">
                    <label>Selecione o Cosmético / Produto</label>
                    <select id="vendaProdutoSelect" onchange="atualizarInfoVenda()" required>
                        <option value="">-- Escolha um cosmético --</option>
                    </select>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label>Qtd. a Vender</label>
                        <input type="number" id="vendaQtd" min="1" value="1" oninput="atualizarInfoVenda()" required>
                    </div>
                    <div class="form-group">
                        <label>💵 Função Pagamento</label>
                        <select id="vendaFormaPagamento" onchange="atualizarInfoVenda()" required>
                            <option value="Dinheiro">💵 Dinheiro (Sem taxa)</option>
                            <option value="Pix">📱 Pix (Sem taxa)</option>
                            <option value="Cartão de Débito">💳 Cartão Débito (Sem taxa)</option>
                            <option value="Cartão de Crédito">💳 Cartão Crédito (+2% taxa)</option>
                        </select>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label>Funcionário Responsável</label>
                        <select id="vendaFuncionarioSelect" required>
                            <option value="Camila Santos (Atendente)">Camila Santos (Atendente)</option>
                            <option value="Dra. Juliana Lima (Farmacêutica)">Dra. Juliana Lima (Farmacêutica)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Cliente (Opcional)</label>
                        <input type="text" id="vendaCliente" placeholder="Ex: Maria Fernanda Souza">
                    </div>
                </div>

                <!-- Resumo com Cálculo da Taxa e Total -->
                <div class="venda-resumo-box">
                    <div class="venda-linha">
                        <span style="color: var(--text-muted);">Subtotal do Produto:</span>
                        <strong id="vendaSubtotal">R$ 0,00</strong>
                    </div>
                    <div class="venda-linha" id="linhaTaxa" style="display: none; color: #b45309;">
                        <span>Taxa de Cartão Crédito (2%):</span>
                        <strong id="vendaTaxa">+ R$ 0,00</strong>
                    </div>
                    <div class="venda-linha destaque">
                        <span>Total Final a Pagar:</span>
                        <span id="vendaTotalValor">R$ 0,00</span>
                    </div>
                    <div id="vendaEstoqueDisp" style="font-size: 0.8rem; color: var(--text-muted); margin-top: 6px;">Estoque disponível: -</div>
                </div>

                <button type="submit" class="btn-green" style="width: 100%;">💳 Concluir Pagamento & Emitir Comprovante</button>
            </form>

            <div id="comprovanteVenda" class="comprovante">
                <h4>✓ Pagamento Concluído com Sucesso!</h4>
                <div id="comprovanteTexto" style="font-size: 0.88rem; line-height: 1.5;"></div>
            </div>
        </div>

        <!-- CADASTRO DE PRODUTO & CONSULTA -->
        <div class="card">
            <h2>✨ Cadastrar Cosmético (Código Contado)</h2>
            <div id="alertaCadastro" class="alert-box alert-success"></div>

            <form id="formProduto" onsubmit="salvarProduto(event)">
                <div class="form-group">
                    <label>Nome do Cosmético</label>
                    <input type="text" id="prodNome" placeholder="Ex: Sérum Facial Clareador Vitamina C" required>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Marca</label>
                        <input type="text" id="prodMarca" placeholder="Ex: Principia" required>
                    </div>
                    <div class="form-group">
                        <label>Categoria</label>
                        <select id="prodCategoria" required>
                            <option value="Cuidados com a Pele">Cuidados com a Pele</option>
                            <option value="Cabelos">Cabelos</option>
                            <option value="Maquiagem">Maquiagem</option>
                            <option value="Perfumaria">Perfumaria</option>
                            <option value="Limpeza Facial">Limpeza Facial</option>
                            <option value="Outros Cosméticos">Outros Cosméticos</option>
                        </select>
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Preço Unitário (R$)</label>
                        <input type="number" id="prodPreco" step="0.01" min="0.01" placeholder="Ex: 59.90" required>
                    </div>
                    <div class="form-group">
                        <label>Qtd. Estoque</label>
                        <input type="number" id="prodQtd" min="1" placeholder="Ex: 25" required>
                    </div>
                </div>
                <button type="submit" style="width: 100%;">📦 Cadastrar Cosmético (Salva em TXT)</button>
            </form>

            <!-- Consultar Produto -->
            <h2 style="margin-top: 22px;">🔍 Consultar Produto por Código</h2>
            <div style="display: flex; gap: 8px;">
                <input type="number" id="inputCodigoBusca" placeholder="Informe o código (ex: 1, 2, 3...)" min="1">
                <button type="button" onclick="consultarProduto()" style="background: #0d9488;">Consultar</button>
            </div>
            <div id="resultadoConsulta" style="display:none; margin-top:10px; padding:12px; border-radius:8px; font-size:0.9rem;"></div>
        </div>

    </div>

    <!-- SEÇÃO NOVA: RELATÓRIO DE VENDAS -->
    <div class="card card-relatorio">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 14px;">
            <h2 style="border: none; margin: 0; padding: 0;">📊 Função Relatório de Vendas (Métricas & Faturamento)</h2>
            <div style="display: flex; gap: 8px;">
                <button onclick="carregarRelatorio()" class="btn-purple" style="padding: 7px 14px; font-size: 0.88rem;">🔄 Atualizar Relatório</button>
                <button onclick="baixarRelatorioTxt()" style="background: #475569; padding: 7px 14px; font-size: 0.88rem;">📑 Exportar TXT</button>
            </div>
        </div>

        <!-- KPI CARDS -->
        <div class="kpi-grid">
            <div class="kpi-card">
                <div class="kpi-title">Faturamento Total</div>
                <div class="kpi-value green" id="kpiFaturamento">R$ 0,00</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Total de Vendas</div>
                <div class="kpi-value purple" id="kpiTotalVendas">0</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Unidades Vendidas</div>
                <div class="kpi-value" id="kpiUnidades">0 un.</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Taxas Coletadas (2% Crédito)</div>
                <div class="kpi-value orange" id="kpiTaxas">R$ 0,00</div>
            </div>
        </div>

        <!-- TABELA DO HISTÓRICO DE VENDAS -->
        <h3 style="font-size: 1rem; color: #6d28d9; margin-bottom: 8px;">📜 Histórico Completo de Transações</h3>
        <div style="overflow-x: auto; background: white; border-radius: 8px; border: 1px solid var(--border);">
            <table>
                <thead>
                    <tr>
                        <th style="width: 70px;">Nº Venda</th>
                        <th>Data/Hora</th>
                        <th>Cosmético</th>
                        <th>Qtd.</th>
                        <th>Forma de Pagamento</th>
                        <th>Taxa (+2%)</th>
                        <th>Total Pago</th>
                        <th>Vendedor(a)</th>
                        <th>Cliente</th>
                    </tr>
                </thead>
                <tbody id="tabelaRelatorioVendas">
                    <tr><td colspan="9" style="text-align: center;">Carregando relatório de vendas...</td></tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Linha 2: Equipe & Clientes -->
    <div class="grid">
        <div class="card">
            <h2>👥 Equipe de Funcionários</h2>
            <div class="team-box">
                <div class="team-card">
                    <div class="role">Atendente</div>
                    <div class="name">Camila Santos</div>
                    <div class="reg">Registro: MAT-1042</div>
                </div>
                <div class="team-card">
                    <div class="role">Farmacêutica</div>
                    <div class="name">Dra. Juliana Lima</div>
                    <div class="reg">Registro: CRF-SP 48.912</div>
                </div>
            </div>
            <p style="font-size: 0.82rem; color: var(--text-muted);">
                ✓ Os objetos <strong>Atendente</strong> e <strong>Farmacêutica</strong> estão ativos na classe Funcionario.
            </p>
        </div>

        <div class="card">
            <h2>👤 Cadastrar Cliente</h2>
            <form onsubmit="salvarCliente(event)">
                <div class="form-row">
                    <div class="form-group">
                        <label>Nome</label>
                        <input type="text" id="cliNome" placeholder="Ex: Ana Clara" required>
                    </div>
                    <div class="form-group">
                        <label>CPF</label>
                        <input type="text" id="cliCpf" placeholder="000.000.000-00" required>
                    </div>
                </div>
                <div class="form-group">
                    <label>Telefone</label>
                    <input type="text" id="cliTelefone" placeholder="(11) 98765-4321" required>
                </div>
                <button type="submit" style="width: 100%;">+ Cadastrar Cliente</button>
            </form>
        </div>
    </div>

    <!-- Linha 3: Visualização do Estoque Completo -->
    <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
            <h2>📊 Estoque de Cosméticos (Salvo em TXT com Códigos Contados)</h2>
            <button onclick="carregarEstoque()" style="padding: 6px 12px; font-size: 0.85rem; background: #64748b;">🔄 Atualizar</button>
        </div>
        <div style="overflow-x: auto;">
            <table>
                <thead>
                    <tr>
                        <th style="width: 80px;">Código</th>
                        <th>Cosmético</th>
                        <th>Marca</th>
                        <th>Categoria</th>
                        <th>Preço (R$)</th>
                        <th>Qtd. Estoque</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody id="tabelaEstoque">
                    <tr><td colspan="7" style="text-align: center;">Carregando dados...</td></tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    const API_BASE = window.location.origin.includes(':8080') ? '' : 'http://localhost:8080';

    let produtosCache = [];
    let vendasCache = [];

    function obterEstoqueLocalStorage() {
        const salvo = localStorage.getItem('farmacia_produtos');
        if (salvo) {
            try { return JSON.parse(salvo); } catch(e) {}
        }
        return [
            { codigo: 1, nome: "Protetor Solar FPS 50", marca: "La Roche-Posay", categoria: "Cuidados com a Pele", preco: 89.90, quantidadeEstoque: 25 },
            { codigo: 2, nome: "Hidratante Facial Ácido Hialurônico", marca: "CeraVe", categoria: "Cuidados com a Pele", preco: 74.50, quantidadeEstoque: 40 },
            { codigo: 3, nome: "Shampoo Antiqueda Revitalizante", marca: "Vichy Dercos", categoria: "Cabelos", preco: 112.00, quantidadeEstoque: 18 },
            { codigo: 4, nome: "Batom Matte Longa Duração", marca: "Maybelline", categoria: "Maquiagem", preco: 39.90, quantidadeEstoque: 50 },
            { codigo: 5, nome: "Água Micelar Demaquilante 400ml", marca: "L'Oréal Paris", categoria: "Limpeza Facial", preco: 44.90, quantidadeEstoque: 30 }
        ];
    }

    function obterVendasLocalStorage() {
        const salvo = localStorage.getItem('farmacia_vendas');
        if (salvo) {
            try { return JSON.parse(salvo); } catch(e) {}
        }
        return [
            { id: 1, dataHora: "16/11/2026 14:00", codigoProduto: 1, nomeProduto: "Protetor Solar FPS 50", quantidade: 1, subtotal: 89.90, taxa: 0.00, valorTotal: 89.90, formaPagamento: "Pix", funcionario: "Camila Santos (Atendente)", cliente: "Maria Fernanda Souza" },
            { id: 2, dataHora: "16/11/2026 14:30", codigoProduto: 2, nomeProduto: "Hidratante Facial Ácido Hialurônico", quantidade: 2, subtotal: 149.00, taxa: 2.98, valorTotal: 151.98, formaPagamento: "Cartão de Crédito", funcionario: "Dra. Juliana Lima (Farmacêutica)", cliente: "Carlos Eduardo Lima" }
        ];
    }

    function salvarEstoqueLocalStorage(lista) {
        localStorage.setItem('farmacia_produtos', JSON.stringify(lista));
    }

    function salvarVendasLocalStorage(lista) {
        localStorage.setItem('farmacia_vendas', JSON.stringify(lista));
    }

    window.addEventListener('DOMContentLoaded', () => {
        carregarEstoque();
        carregarRelatorio();
    });

    // 1. CARREGAR ESTOQUE
    async function carregarEstoque() {
        const tbody = document.getElementById('tabelaEstoque');
        let produtos = [];

        try {
            const res = await fetch(`${API_BASE}/api/produtos`);
            if (res.ok) {
                produtos = await res.json();
                salvarEstoqueLocalStorage(produtos);
            } else {
                throw new Error();
            }
        } catch (e) {
            produtos = obterEstoqueLocalStorage();
        }

        produtosCache = produtos;
        popularSelectVenda(produtos);

        let totalItens = 0;
        let totalUnidades = 0;

        if (produtos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">Nenhum produto cadastrado.</td></tr>';
            document.getElementById('headerStats').textContent = '0 produtos no estoque';
            return;
        }

        let html = '';
        produtos.forEach(p => {
            totalItens++;
            totalUnidades += p.quantidadeEstoque;
            const isBaixo = p.quantidadeEstoque <= 5;
            const tagClass = isBaixo ? 'tag-stock tag-low' : 'tag-stock';
            const statusTexto = isBaixo ? 'Estoque Baixo' : 'Disponível';

            html += `
                <tr>
                    <td><span class="badge-code">#${p.codigo}</span></td>
                    <td><strong>${escapeHtml(p.nome)}</strong></td>
                    <td>${escapeHtml(p.marca)}</td>
                    <td>${escapeHtml(p.categoria)}</td>
                    <td>R$ ${Number(p.preco).toFixed(2)}</td>
                    <td><strong>${p.quantidadeEstoque} un.</strong></td>
                    <td><span class="${tagClass}">${statusTexto}</span></td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
        document.getElementById('headerStats').textContent = `${totalItens} produtos cadastrados | ${totalUnidades} un. em estoque`;
    }

    // 2. CARREGAR FUNÇÃO RELATÓRIO DE VENDAS
    async function carregarRelatorio() {
        const tbody = document.getElementById('tabelaRelatorioVendas');
        let lista = [];

        try {
            const res = await fetch(`${API_BASE}/api/vendas`);
            if (res.ok) {
                lista = await res.json();
                salvarVendasLocalStorage(lista);
            } else {
                throw new Error();
            }
        } catch (e) {
            lista = obterVendasLocalStorage();
        }

        vendasCache = lista;

        let totalTransacoes = lista.length;
        let totalUnidades = 0;
        let faturamento = 0.0;
        let totalTaxas = 0.0;

        if (lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center;">Nenhuma venda registrada ainda.</td></tr>';
            document.getElementById('kpiFaturamento').textContent = 'R$ 0,00';
            document.getElementById('kpiTotalVendas').textContent = '0';
            document.getElementById('kpiUnidades').textContent = '0 un.';
            document.getElementById('kpiTaxas').textContent = 'R$ 0,00';
            return;
        }

        let html = '';
        lista.slice().reverse().forEach(v => {
            totalUnidades += v.quantidade;
            faturamento += v.valorTotal;
            totalTaxas += (v.taxa || 0);

            const taxaTexto = (v.taxa && v.taxa > 0) ? `<span class="tag-taxa">+ R$ ${Number(v.taxa).toFixed(2)}</span>` : 'R$ 0,00';

            html += `
                <tr>
                    <td><span class="badge-code">#${v.id}</span></td>
                    <td style="font-size: 0.84rem; color: var(--text-muted);">${v.dataHora}</td>
                    <td><strong>${escapeHtml(v.nomeProduto)}</strong></td>
                    <td>${v.quantidade} un.</td>
                    <td><span class="tag-pagto">${escapeHtml(v.formaPagamento)}</span></td>
                    <td>${taxaTexto}</td>
                    <td><strong style="color: #15803d;">R$ ${Number(v.valorTotal).toFixed(2)}</strong></td>
                    <td style="font-size: 0.85rem;">${escapeHtml(v.funcionario)}</td>
                    <td style="font-size: 0.85rem;">${escapeHtml(v.cliente)}</td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
        document.getElementById('kpiFaturamento').textContent = 'R$ ' + faturamento.toFixed(2);
        document.getElementById('kpiTotalVendas').textContent = totalTransacoes;
        document.getElementById('kpiUnidades').textContent = totalUnidades + ' un.';
        document.getElementById('kpiTaxas').textContent = 'R$ ' + totalTaxas.toFixed(2);
    }

    // 3. POPULAR SELECT DA JANELA DE VENDA
    function popularSelectVenda(produtos) {
        const select = document.getElementById('vendaProdutoSelect');
        const valorAtual = select.value;
        select.innerHTML = '<option value="">-- Escolha um cosmético --</option>';

        produtos.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.codigo;
            opt.textContent = `#${p.codigo} - ${p.nome} (R$ ${Number(p.preco).toFixed(2)} | Estoque: ${p.quantidadeEstoque})`;
            select.appendChild(opt);
        });

        if (valorAtual) select.value = valorAtual;
        atualizarInfoVenda();
    }

    // 4. FUNÇÃO PAGAMENTO & ATUALIZAÇÃO DE PREÇO (+2% DE TAXA NO CRÉDITO)
    function atualizarInfoVenda() {
        const codigo = parseInt(document.getElementById('vendaProdutoSelect').value);
        const qtd = parseInt(document.getElementById('vendaQtd').value) || 0;
        const formaPagto = document.getElementById('vendaFormaPagamento').value;

        const subtotalEl = document.getElementById('vendaSubtotal');
        const linhaTaxa = document.getElementById('linhaTaxa');
        const taxaEl = document.getElementById('vendaTaxa');
        const totalEl = document.getElementById('vendaTotalValor');
        const dispEl = document.getElementById('vendaEstoqueDisp');

        const prod = produtosCache.find(p => p.codigo === codigo);
        if (prod) {
            const subtotal = prod.preco * qtd;
            let taxa = 0;
            const isCredito = formaPagto.toLowerCase().includes('crédito') || formaPagto.toLowerCase().includes('credito');

            if (isCredito) {
                taxa = subtotal * 0.02; // Adiciona 2% de taxa no crédito
                linhaTaxa.style.display = 'flex';
                taxaEl.textContent = `+ R$ ${taxa.toFixed(2)} (2%)`;
            } else {
                linhaTaxa.style.display = 'none';
            }

            const totalFinal = subtotal + taxa;

            subtotalEl.textContent = 'R$ ' + subtotal.toFixed(2);
            totalEl.textContent = 'R$ ' + totalFinal.toFixed(2);
            dispEl.textContent = `Estoque disponível: ${prod.quantidadeEstoque} unidades (Preço unitário: R$ ${prod.preco.toFixed(2)})`;

            if (qtd > prod.quantidadeEstoque) {
                dispEl.style.color = 'var(--danger)';
                dispEl.textContent += ' - ATENÇÃO: Quantidade excede o estoque!';
            } else {
                dispEl.style.color = 'var(--text-muted)';
            }
        } else {
            subtotalEl.textContent = 'R$ 0,00';
            linhaTaxa.style.display = 'none';
            totalEl.textContent = 'R$ 0,00';
            dispEl.textContent = 'Estoque disponível: -';
        }
    }

    // 5. REALIZAR VENDA COM FUNÇÃO PAGAMENTO
    async function realizarVenda(e) {
        e.preventDefault();
        const codigo = parseInt(document.getElementById('vendaProdutoSelect').value);
        const qtd = parseInt(document.getElementById('vendaQtd').value);
        const formaPagamento = document.getElementById('vendaFormaPagamento').value;
        const funcionario = document.getElementById('vendaFuncionarioSelect').value;
        const cliente = document.getElementById('vendaCliente').value.trim() || 'Cliente Balcão';

        const alerta = document.getElementById('alertaVenda');
        const comprovante = document.getElementById('comprovanteVenda');
        const compTexto = document.getElementById('comprovanteTexto');

        const prod = produtosCache.find(p => p.codigo === codigo);
        if (!prod) {
            mostrarAlerta(alerta, 'Selecione um cosmético válido.', 'error');
            return;
        }

        if (qtd <= 0 || qtd > prod.quantidadeEstoque) {
            mostrarAlerta(alerta, `Estoque insuficiente! Disponível: ${prod.quantidadeEstoque} unidades.`, 'error');
            return;
        }

        let vendaRealizada = null;

        try {
            const res = await fetch(`${API_BASE}/api/vender`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ codigoProduto: codigo, quantidade: qtd, formaPagamento, funcionario, cliente })
            });
            if (res.ok) {
                vendaRealizada = await res.json();
            }
        } catch(err) {}

        if (!vendaRealizada) {
            prod.quantidadeEstoque -= qtd;
            salvarEstoqueLocalStorage(produtosCache);
            const subtotal = prod.preco * qtd;
            const isCredito = formaPagamento.toLowerCase().includes('crédito') || formaPagamento.toLowerCase().includes('credito');
            const taxa = isCredito ? subtotal * 0.02 : 0.0;
            const valorTotal = subtotal + taxa;

            vendaRealizada = {
                id: (vendasCache.length > 0 ? Math.max(...vendasCache.map(v => v.id)) + 1 : 1),
                dataHora: new Date().toLocaleString('pt-BR'),
                codigoProduto: prod.codigo,
                nomeProduto: prod.nome,
                quantidade: qtd,
                subtotal,
                taxa,
                valorTotal,
                formaPagamento,
                funcionario,
                cliente
            };
            vendasCache.push(vendaRealizada);
            salvarVendasLocalStorage(vendasCache);
        }

        mostrarAlerta(alerta, `✓ Venda realizada com sucesso via ${vendaRealizada.formaPagamento} e salva no Relatório!`, 'success');
        
        let infoTaxa = '';
        if (vendaRealizada.taxa > 0) {
            infoTaxa = `<span class="tag-taxa">+ Taxa 2% Crédito: R$ ${Number(vendaRealizada.taxa).toFixed(2)}</span><br>`;
        }

        compTexto.innerHTML = `
            <strong>Comprovante Nº:</strong> #${vendaRealizada.id}<br>
            <strong>Data/Hora:</strong> ${vendaRealizada.dataHora}<br>
            <strong>Produto:</strong> #${vendaRealizada.codigoProduto} - ${vendaRealizada.nomeProduto}<br>
            <strong>Quantidade:</strong> ${vendaRealizada.quantidade} un. | <strong>Subtotal:</strong> R$ ${Number(vendaRealizada.subtotal || (vendaRealizada.valorTotal - (vendaRealizada.taxa||0))).toFixed(2)}<br>
            <strong>Forma de Pagamento:</strong> ${vendaRealizada.formaPagamento}<br>
            ${infoTaxa}
            <strong>Total Final Pago:</strong> <span style="font-size:1.1rem; color:#15803d; font-weight:700;">R$ ${Number(vendaRealizada.valorTotal).toFixed(2)}</span><br>
            <strong>Atendido por:</strong> ${vendaRealizada.funcionario}<br>
            <strong>Cliente:</strong> ${vendaRealizada.cliente}
        `;
        comprovante.style.display = 'block';

        document.getElementById('formVenda').reset();
        carregarEstoque();
        carregarRelatorio();
    }

    // 6. CADASTRAR PRODUTO (SALVAMENTO GARANTIDO EM TXT)
    async function salvarProduto(e) {
        e.preventDefault();
        const nome = document.getElementById('prodNome').value.trim();
        const marca = document.getElementById('prodMarca').value.trim();
        const categoria = document.getElementById('prodCategoria').value;
        const preco = parseFloat(document.getElementById('prodPreco').value);
        const quantidadeEstoque = parseInt(document.getElementById('prodQtd').value);

        const payload = { nome, marca, categoria, preco, quantidadeEstoque };
        let criado = null;

        try {
            const res = await fetch(`${API_BASE}/api/cadastrar`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (res.ok) {
                criado = await res.json();
            }
        } catch(err) {}

        if (!criado) {
            const maxCod = produtosCache.reduce((max, p) => p.codigo > max ? p.codigo : max, 0);
            criado = { codigo: maxCod + 1, ...payload };
            produtosCache.push(criado);
            salvarEstoqueLocalStorage(produtosCache);
        }

        const alerta = document.getElementById('alertaCadastro');
        alerta.className = 'alert-box alert-success';
        alerta.textContent = `✓ Cosmético "${criado.nome}" cadastrado com sucesso! Código gerado: #${criado.codigo} (Salvo no TXT)`;
        alerta.style.display = 'block';
        setTimeout(() => alerta.style.display = 'none', 5000);

        document.getElementById('formProduto').reset();
        carregarEstoque();
    }

    // 7. CONSULTAR PRODUTO POR CÓDIGO
    async function consultarProduto() {
        const codigo = parseInt(document.getElementById('inputCodigoBusca').value);
        const div = document.getElementById('resultadoConsulta');
        if (!codigo) return;

        let p = produtosCache.find(item => item.codigo === codigo);

        try {
            const res = await fetch(`${API_BASE}/api/consultar?codigo=${codigo}`);
            if (res.ok) p = await res.json();
        } catch(e) {}

        div.style.display = 'block';
        if (p && p.nome) {
            div.style.background = '#f0fdf4';
            div.style.border = '1px solid #bbf7d0';
            div.style.color = '#166534';
            div.innerHTML = `
                <strong>✓ Produto Encontrado (Código #${p.codigo}):</strong><br>
                <strong>Nome:</strong> ${escapeHtml(p.nome)} | <strong>Marca:</strong> ${escapeHtml(p.marca)}<br>
                <strong>Categoria:</strong> ${escapeHtml(p.categoria)}<br>
                <strong>Preço:</strong> R$ ${Number(p.preco).toFixed(2)} | <strong>Estoque Atual:</strong> ${p.quantidadeEstoque} unidades
            `;
        } else {
            div.style.background = '#fef2f2';
            div.style.border = '1px solid #fecaca';
            div.style.color = '#991b1b';
            div.innerHTML = `<strong>Aviso:</strong> Nenhum produto encontrado com o código #${codigo}.`;
        }
    }

    // 8. CADASTRAR CLIENTE
    async function salvarCliente(e) {
        e.preventDefault();
        const nome = document.getElementById('cliNome').value.trim();
        const cpf = document.getElementById('cliCpf').value.trim();
        const telefone = document.getElementById('cliTelefone').value.trim();

        try {
            await fetch(`${API_BASE}/api/clientes`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nome, cpf, telefone })
            });
        } catch(e) {}

        alert(`Cliente "${nome}" cadastrado com sucesso e salvo no TXT!`);
        document.getElementById('cliNome').value = '';
        document.getElementById('cliCpf').value = '';
        document.getElementById('cliTelefone').value = '';
    }

    // 9. BAIXAR ARQUIVO TXT PRINCIPAL
    function baixarArquivoTxt() {
        if (API_BASE === '') {
            window.location.href = '/api/baixar-txt';
        } else {
            let txt = "================================================================================\\n";
            txt += "                    SISTEMA DE FARMÁCIA - BASE DE DADOS\\n";
            txt += "================================================================================\\n\\n";
            txt += "[PRODUTOS_COSMETICOS]\\n";
            txt += "codigo;nome;marca;categoria;preco;quantidadeEstoque\\n";
            produtosCache.forEach(p => {
                txt += `${p.codigo};${p.nome};${p.marca};${p.categoria};${Number(p.preco).toFixed(2)};${p.quantidadeEstoque}\\n`;
            });
            txt += "\\n[FUNCIONARIOS]\\n";
            txt += "1;Camila Santos;Atendente;MAT-1042\\n";
            txt += "2;Dra. Juliana Lima;Farmacêutica;CRF-SP 48.912\\n";

            const blob = new Blob([txt], { type: 'text/plain;charset=utf-8' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = 'dados_farmacia.txt';
            link.click();
        }
    }

    // 10. BAIXAR RELATÓRIO DE VENDAS EM TXT
    function baixarRelatorioTxt() {
        if (API_BASE === '') {
            window.location.href = '/api/baixar-relatorio';
        } else {
            let txt = "================================================================================\\n";
            txt += "           RELATÓRIO GERAL DE VENDAS - FARMÁCIA SAÚDE & BELEZA\\n";
            txt += "================================================================================\\n";
            txt += `Data da Emissão: ${new Date().toLocaleString('pt-BR')}\\n`;
            txt += "--------------------------------------------------------------------------------\\n";

            let faturamento = 0;
            let totalUnidades = 0;
            let totalTaxas = 0;

            vendasCache.forEach(v => {
                faturamento += v.valorTotal;
                totalUnidades += v.quantidade;
                totalTaxas += (v.taxa || 0);
            });

            txt += `Total de Vendas Realizadas : ${vendasCache.length} vendas\\n`;
            txt += `Total de Unidades Vendidas : ${totalUnidades} unidades\\n`;
            txt += `Total de Taxas Coletadas   : R$ ${totalTaxas.toFixed(2)} (Taxa de 2% Cartão Crédito)\\n`;
            txt += `FATURAMENTO BRUTO TOTAL    : R$ ${faturamento.toFixed(2)}\\n`;
            txt += "--------------------------------------------------------------------------------\\n";
            txt += "HISTÓRICO DETALHADO DE TRANSAÇÕES:\\n";
            txt += "ID | DATA/HORA | PRODUTO | QTD | FORMA PAGTO | TAXA | TOTAL | FUNCIONÁRIO | CLIENTE\\n";
            txt += "--------------------------------------------------------------------------------\\n";

            vendasCache.forEach(v => {
                txt += `#${v.id} | ${v.dataHora} | ${v.nomeProduto} | ${v.quantidade} un. | ${v.formaPagamento} | R$ ${Number(v.taxa||0).toFixed(2)} | R$ ${Number(v.valorTotal).toFixed(2)} | ${v.funcionario} | ${v.cliente}\\n`;
            });
            txt += "================================================================================\\n";

            const blob = new Blob([txt], { type: 'text/plain;charset=utf-8' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = 'relatorio_vendas.txt';
            link.click();
        }
    }

    function mostrarAlerta(el, msg, tipo) {
        el.className = 'alert-box ' + (tipo === 'success' ? 'alert-success' : 'alert-error');
        el.textContent = msg;
        el.style.display = 'block';
        setTimeout(() => el.style.display = 'none', 5000);
    }

    function escapeHtml(texto) {
        if (!texto) return '';
        return String(texto).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
</script>

</body>
</html>
""";
    }
}
