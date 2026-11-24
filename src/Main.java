import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static Biblioteca biblioteca;

    public static void main(String[] args) {
        Path arquivo = Paths.get(args.length > 0 ? args[0] : "biblioteca.dat");
        biblioteca = new Biblioteca(arquivo);

        System.out.println("==============================================");
        System.out.println("   Bem-vindo(a) à Biblioteca Digital");
        System.out.println("==============================================");

        carregarDados(arquivo);

        try {
            boolean executando = true;
            while (executando) {
                exibirMenu();
                Integer opcao = lerInteiro("Escolha uma opção: ");
                if (opcao == null) {
                    continue;
                }
                try {
                    executando = executar(opcao);
                } catch (LivroIndisponivelException e) {
                    System.out.println("Empréstimo recusado: " + e.getMessage());
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("Erro: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada encerrada.");
        } finally {
            // Qualquer que seja o motivo da saída, os dados são gravados
            salvarDados();
            System.out.println("Sistema encerrado. Até logo!");
        }
    }

    private static void exibirMenu() {
        System.out.println();
        System.out.println("---------------- MENU ----------------");
        System.out.println(" Livros");
        System.out.println("   1 - Cadastrar livro");
        System.out.println("   2 - Listar livros");
        System.out.println("   3 - Buscar livro por título");
        System.out.println("   4 - Ordenar livros (título ou ano)");
        System.out.println("   5 - Listar livros de um autor");
        System.out.println("   6 - Agrupar livros por autor");
        System.out.println("   7 - Listar livros disponíveis");
        System.out.println("   8 - Listar livros emprestados");
        System.out.println(" Usuários");
        System.out.println("   9 - Cadastrar usuário");
        System.out.println("  10 - Listar usuários");
        System.out.println("  11 - Buscar usuário por e-mail");
        System.out.println(" Empréstimos");
        System.out.println("  12 - Realizar empréstimo");
        System.out.println("  13 - Devolver livro");
        System.out.println("  14 - Histórico de empréstimos");
        System.out.println("   0 - Sair");
        System.out.println("--------------------------------------");
    }

    /** Executa a opção escolhida. Retorna false quando o usuário pede para sair. */
    private static boolean executar(int opcao) throws LivroIndisponivelException {
        switch (opcao) {
            case 1: cadastrarLivro(); break;
            case 2: imprimirLivros("Todos os livros:", biblioteca.listarLivros()); break;
            case 3: buscarLivro(); break;
            case 4: ordenarLivros(); break;
            case 5: livrosDeUmAutor(); break;
            case 6: agruparPorAutor(); break;
            case 7: imprimirLivros("Livros disponíveis:", biblioteca.livrosDisponiveis()); break;
            case 8: imprimirLivros("Livros emprestados:", biblioteca.livrosEmprestados()); break;
            case 9: cadastrarUsuario(); break;
            case 10: listarUsuarios(); break;
            case 11: buscarUsuario(); break;
            case 12: realizarEmprestimo(); break;
            case 13: devolverLivro(); break;
            case 14: exibirHistorico(); break;
            case 0: return false;
            default: System.out.println("Opção inválida! Escolha um número entre 0 e 14.");
        }
        return true;
    }

    // ------------------------------------------------------------------ Livros

    private static void cadastrarLivro() {
        String titulo = lerTexto("Título: ");
        String autor = lerTexto("Autor: ");
        Integer ano = lerInteiro("Ano de publicação: ");
        if (ano == null) {
            return;
        }
        Livro livro = biblioteca.cadastrarLivro(titulo, autor, ano);
        System.out.println("Livro cadastrado: " + livro);
        salvarDados();
    }

    private static void buscarLivro() {
        String trecho = lerTexto("Título (ou parte dele): ");
        imprimirLivros("Resultado da busca:", biblioteca.buscarPorTitulo(trecho));
    }

    private static void ordenarLivros() {
        System.out.println("Ordenar por: 1 - Título | 2 - Ano de publicação");
        Integer criterio = lerInteiro("Critério: ");
        if (criterio == null) {
            return;
        }
        if (criterio == 1) {
            imprimirLivros("Livros ordenados por título:", biblioteca.ordenarPorTitulo());
        } else if (criterio == 2) {
            imprimirLivros("Livros ordenados por ano:", biblioteca.ordenarPorAno());
        } else {
            System.out.println("Critério inválido! Escolha 1 ou 2.");
        }
    }

    private static void livrosDeUmAutor() {
        String autor = lerTexto("Autor: ");
        imprimirLivros("Livros de " + autor + ":", biblioteca.livrosDoAutor(autor));
    }

    private static void agruparPorAutor() {
        Map<String, List<Livro>> porAutor = biblioteca.agruparPorAutor();
        if (porAutor.isEmpty()) {
            System.out.println("Nenhum livro cadastrado.");
            return;
        }
        porAutor.forEach((autor, lista) -> {
            System.out.println(autor + " (" + lista.size() + "):");
            lista.forEach(l -> System.out.println("  - " + l));
        });
    }

    private static void imprimirLivros(String titulo, List<Livro> lista) {
        if (lista.isEmpty()) {
            System.out.println("Nenhum livro encontrado.");
            return;
        }
        System.out.println(titulo);
        lista.forEach(l -> System.out.println("  - " + l));
    }

    // ---------------------------------------------------------------- Usuários

    private static void cadastrarUsuario() {
        String nome = lerTexto("Nome: ");
        String email = lerTexto("E-mail: ");
        Usuario usuario = biblioteca.cadastrarUsuario(nome, email);
        System.out.println("Usuário cadastrado: " + usuario);
        salvarDados();
    }

    private static void listarUsuarios() {
        List<Usuario> usuarios = biblioteca.listarUsuarios();
        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
            return;
        }
        System.out.println("Usuários:");
        usuarios.forEach(u -> System.out.println("  - " + u));
    }

    private static void buscarUsuario() {
        String trecho = lerTexto("E-mail (ou parte dele): ");
        List<Usuario> encontrados = biblioteca.buscarUsuariosPorEmail(trecho);
        if (encontrados.isEmpty()) {
            System.out.println("Nenhum usuário encontrado.");
            return;
        }
        System.out.println("Resultado da busca:");
        encontrados.stream().map(Usuario::toString).forEach(texto -> System.out.println("  - " + texto));
    }

    // ------------------------------------------------------------- Empréstimos

    private static void realizarEmprestimo() throws LivroIndisponivelException {
        String titulo = lerTexto("Título do livro: ");
        String email = lerTexto("E-mail do usuário: ");
        Emprestimo emprestimo = biblioteca.emprestar(titulo, email);
        System.out.println("Empréstimo realizado: " + emprestimo);
        salvarDados();
    }

    private static void devolverLivro() {
        String titulo = lerTexto("Título do livro: ");
        Livro livro = biblioteca.devolver(titulo);
        System.out.println("Livro devolvido: " + livro);
        salvarDados();
    }

    private static void exibirHistorico() {
        List<Emprestimo> historico = biblioteca.historicoDeEmprestimos();
        if (historico.isEmpty()) {
            System.out.println("Nenhum empréstimo registrado.");
            return;
        }
        System.out.println("Histórico de empréstimos:");
        historico.forEach(e -> System.out.println("  - " + e));
    }

    // ------------------------------------------------------------- Persistência

    private static void carregarDados(Path arquivo) {
        try {
            if (biblioteca.carregar()) {
                System.out.println("Dados carregados de " + arquivo + ": "
                        + biblioteca.listarLivros().size() + " livro(s) e "
                        + biblioteca.listarUsuarios().size() + " usuário(s).");
            } else {
                System.out.println("Nenhum arquivo de dados encontrado. Começando com a biblioteca vazia.");
            }
        } catch (IOException e) {
            System.out.println("Aviso: " + e.getMessage());
            System.out.println("Começando com a biblioteca vazia.");
        }
    }

    private static void salvarDados() {
        try {
            biblioteca.salvar();
        } catch (IOException e) {
            System.out.println("Aviso: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ Entrada

    private static String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return scanner.nextLine().trim();
    }

    private static Integer lerInteiro(String mensagem) {
        System.out.print(mensagem);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida! Digite um número inteiro.");
            return null;
        }
    }
}