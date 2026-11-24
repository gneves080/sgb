import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verificações automáticas da lógica da biblioteca (sem frameworks externos).
 * Execute com: java TesteBiblioteca
 */
public class TesteBiblioteca {

    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        Path arquivo = Files.createTempFile("biblioteca-teste", ".dat");
        try {
            testar(arquivo);
        } finally {
            Files.deleteIfExists(arquivo);
        }
        System.out.println();
        System.out.println(falhas == 0 ? "Todas as verificações passaram." : falhas + " verificação(ões) falharam.");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    private static void testar(Path arquivo) throws Exception {
        Biblioteca b = new Biblioteca(arquivo);

        System.out.println("== Livros ==");
        b.cadastrarLivro("Dom Casmurro", "Machado de Assis", 1899);
        b.cadastrarLivro("Memórias Póstumas de Brás Cubas", "Machado de Assis", 1881);
        b.cadastrarLivro("O Cortiço", "Aluísio Azevedo", 1890);
        b.cadastrarLivro("Capitães da Areia", "Jorge Amado", 1937);
        b.cadastrarLivro("Vidas Secas", "Graciliano Ramos", 1938);
        verificar("5 livros cadastrados", b.listarLivros().size() == 5);
        verificar("título duplicado (ignorando maiúsculas) é rejeitado", lancaIllegalArgument(() -> b.cadastrarLivro("dom casmurro", "X", 1900)));
        verificar("ano futuro é rejeitado", lancaIllegalArgument(() -> b.cadastrarLivro("Futuro", "X", 3000)));
        verificar("título vazio é rejeitado", lancaIllegalArgument(() -> b.cadastrarLivro("  ", "X", 1900)));

        verificar("busca por trecho ignora maiúsculas ('CASMURRO')", titulos(b.buscarPorTitulo("CASMURRO")).equals(Arrays.asList("Dom Casmurro")));
        verificar("busca sem resultado devolve lista vazia", b.buscarPorTitulo("inexistente").isEmpty());
        verificar("ordenar por título (Comparable)", titulos(b.ordenarPorTitulo()).equals(Arrays.asList(
                "Capitães da Areia", "Dom Casmurro", "Memórias Póstumas de Brás Cubas", "O Cortiço", "Vidas Secas")));
        verificar("ordenar por ano (lambda)", titulos(b.ordenarPorAno()).equals(Arrays.asList(
                "Memórias Póstumas de Brás Cubas", "O Cortiço", "Dom Casmurro", "Capitães da Areia", "Vidas Secas")));
        verificar("ordenar não altera a lista original", b.listarLivros().get(0).getTitulo().equals("Dom Casmurro"));
        verificar("livros de um autor (filter)", b.livrosDoAutor("machado de assis").size() == 2);
        Map<String, List<Livro>> porAutor = b.agruparPorAutor();
        verificar("agrupar por autor (4 autores, Machado com 2)", porAutor.size() == 4 && porAutor.get("Machado de Assis").size() == 2);

        System.out.println("\n== Usuários ==");
        b.cadastrarUsuario("Maria Silva", "maria@email.com");
        b.cadastrarUsuario("João Souza", "joao@outro.com");
        verificar("2 usuários cadastrados", b.listarUsuarios().size() == 2);
        verificar("e-mail duplicado é rejeitado", lancaIllegalArgument(() -> b.cadastrarUsuario("Outra", "MARIA@email.com")));
        verificar("e-mail inválido é rejeitado", lancaIllegalArgument(() -> b.cadastrarUsuario("Sem Arroba", "invalido")));
        verificar("busca de usuário por e-mail (trecho)", b.buscarUsuariosPorEmail("outro").size() == 1);
        verificar("busca de e-mails com map + filter", b.buscarEmails("EMAIL").equals(Arrays.asList("maria@email.com")));

        System.out.println("\n== Empréstimos ==");
        b.emprestar("dom casmurro", "maria@email.com");
        verificar("livro emprestado fica indisponível", b.buscarLivroPorTitulo("Dom Casmurro").get().isEmprestado());
        verificar("emprestar de novo lança LivroIndisponivelException", lancaIndisponivel(b, "Dom Casmurro", "joao@outro.com"));
        verificar("livro inexistente é rejeitado", lancaIllegalArgument(() -> b.emprestar("Fantasma", "joao@outro.com")));
        verificar("usuário inexistente é rejeitado", lancaIllegalArgument(() -> b.emprestar("Vidas Secas", "ninguem@email.com")));
        verificar("1 livro emprestado e 4 disponíveis", b.livrosEmprestados().size() == 1 && b.livrosDisponiveis().size() == 4);

        b.devolver("Dom Casmurro");
        verificar("devolução libera o livro", b.livrosEmprestados().isEmpty());
        verificar("devolver livro não emprestado lança IllegalStateException", lancaIllegalState(b, "Dom Casmurro"));
        verificar("histórico tem 1 registro, já devolvido", b.historicoDeEmprestimos().size() == 1
                && !b.historicoDeEmprestimos().get(0).isEmAberto());

        System.out.println("\n== Persistência ==");
        b.emprestar("Vidas Secas", "joao@outro.com");
        b.salvar();
        verificar("arquivo .dat foi criado", Files.size(arquivo) > 0);

        Biblioteca recarregada = new Biblioteca(arquivo);
        verificar("carregar devolve true quando o arquivo existe", recarregada.carregar());
        verificar("livros e usuários recarregados", recarregada.listarLivros().size() == 5 && recarregada.listarUsuarios().size() == 2);
        verificar("estado de empréstimo preservado", recarregada.buscarLivroPorTitulo("Vidas Secas").get().isEmprestado());
        verificar("histórico preservado (2 registros)", recarregada.historicoDeEmprestimos().size() == 2);
        recarregada.devolver("Vidas Secas");
        verificar("após recarregar, devolver fecha o registro do histórico (referências preservadas)",
                recarregada.historicoDeEmprestimos().stream().noneMatch(Emprestimo::isEmAberto));

        Path inexistente = arquivo.resolveSibling("nao-existe-" + System.nanoTime() + ".dat");
        verificar("arquivo inexistente: carregar devolve false", !new Biblioteca(inexistente).carregar());

        Files.write(arquivo, "isto não é um objeto serializado".getBytes(StandardCharsets.UTF_8));
        verificar("arquivo corrompido lança IOException e não altera os dados", lancaIOException(new Biblioteca(arquivo)));
    }

    // ---------------------------------------------------------------- Auxiliares

    private static List<String> titulos(List<Livro> livros) {
        return livros.stream().map(Livro::getTitulo).collect(Collectors.toList());
    }

    /** Como Runnable, mas permite que a ação lance exceções verificadas. */
    @FunctionalInterface
    private interface Acao {
        void executar() throws Exception;
    }

    private static boolean lancaIllegalArgument(Acao acao) {
        try {
            acao.executar();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean lancaIndisponivel(Biblioteca b, String titulo, String email) {
        try {
            b.emprestar(titulo, email);
            return false;
        } catch (LivroIndisponivelException e) {
            return true;
        }
    }

    private static boolean lancaIllegalState(Biblioteca b, String titulo) {
        try {
            b.devolver(titulo);
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }

    private static boolean lancaIOException(Biblioteca b) {
        try {
            b.carregar();
            return false;
        } catch (IOException e) {
            return b.listarLivros().isEmpty();
        }
    }

    private static void verificar(String descricao, boolean condicao) {
        if (!condicao) {
            falhas++;
        }
        System.out.println("  [" + (condicao ? "OK" : "FALHA") + "] " + descricao);
    }
}