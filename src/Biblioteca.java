import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Biblioteca {

    private static final String PADRAO_EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final List<Livro> livros = new ArrayList<>();
    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Emprestimo> historico = new ArrayList<>();
    private final Path arquivo;

    public Biblioteca(Path arquivo) {
        this.arquivo = arquivo;
    }

    // ------------------------------------------------------------------ Livros

    public Livro cadastrarLivro(String titulo, String autor, int anoPublicacao) {
        if (vazio(titulo) || vazio(autor)) {
            throw new IllegalArgumentException("Título e autor são obrigatórios.");
        }
        if (anoPublicacao < 1 || anoPublicacao > Year.now().getValue()) {
            throw new IllegalArgumentException("Ano de publicação inválido: " + anoPublicacao + ".");
        }
        if (buscarLivroPorTitulo(titulo).isPresent()) {
            throw new IllegalArgumentException("Já existe um livro com o título \"" + titulo.trim() + "\".");
        }
        Livro livro = new Livro(titulo.trim(), autor.trim(), anoPublicacao);
        livros.add(livro);
        return livro;
    }

    public List<Livro> listarLivros() {
        return Collections.unmodifiableList(livros);
    }

    /** Busca exata pelo título, sem diferenciar maiúsculas de minúsculas. */
    public Optional<Livro> buscarLivroPorTitulo(String titulo) {
        if (titulo == null) {
            return Optional.empty();
        }
        return livros.stream()
                .filter(l -> l.getTitulo().equalsIgnoreCase(titulo.trim()))
                .findFirst();
    }

    /** Busca livros cujo título contenha o trecho informado, sem diferenciar maiúsculas de minúsculas. */
    public List<Livro> buscarPorTitulo(String trecho) {
        String alvo = trecho == null ? "" : trecho.trim().toLowerCase();
        return livros.stream()
                .filter(l -> l.getTitulo().toLowerCase().contains(alvo))
                .collect(Collectors.toList());
    }

    /** Ordem natural (Comparable): por título. Devolve uma nova lista, sem alterar a original. */
    public List<Livro> ordenarPorTitulo() {
        return livros.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Por ano de publicação (mais antigo primeiro); empates são desfeitos pelo título. */
    public List<Livro> ordenarPorAno() {
        return livros.stream()
                .sorted((a, b) -> {
                    int porAno = Integer.compare(a.getAnoPublicacao(), b.getAnoPublicacao());
                    return porAno != 0 ? porAno : a.compareTo(b);
                })
                .collect(Collectors.toList());
    }

    public List<Livro> livrosDoAutor(String autor) {
        String alvo = autor == null ? "" : autor.trim();
        return livros.stream()
                .filter(l -> l.getAutor().equalsIgnoreCase(alvo))
                .collect(Collectors.toList());
    }

    /** Agrupa os livros por autor, com os autores em ordem alfabética. */
    public Map<String, List<Livro>> agruparPorAutor() {
        return livros.stream()
                .collect(Collectors.groupingBy(
                        Livro::getAutor,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                        Collectors.toList()));
    }

    public List<Livro> livrosDisponiveis() {
        return livros.stream()
                .filter(Livro::isDisponivel)
                .collect(Collectors.toList());
    }

    public List<Livro> livrosEmprestados() {
        return livros.stream()
                .filter(Livro::isEmprestado)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- Usuários

    public Usuario cadastrarUsuario(String nome, String email) {
        if (vazio(nome) || vazio(email)) {
            throw new IllegalArgumentException("Nome e e-mail são obrigatórios.");
        }
        if (!email.trim().matches(PADRAO_EMAIL)) {
            throw new IllegalArgumentException("E-mail inválido: " + email.trim() + ".");
        }
        if (buscarUsuarioPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com o e-mail " + email.trim() + ".");
        }
        Usuario usuario = new Usuario(nome.trim(), email.trim());
        usuarios.add(usuario);
        return usuario;
    }

    public List<Usuario> listarUsuarios() {
        return Collections.unmodifiableList(usuarios);
    }

    /** Busca exata pelo e-mail, sem diferenciar maiúsculas de minúsculas. */
    public Optional<Usuario> buscarUsuarioPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return usuarios.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    /** Usuários cujo e-mail contém o trecho informado (sem diferenciar maiúsculas de minúsculas). */
    public List<Usuario> buscarUsuariosPorEmail(String trecho) {
        String alvo = trecho == null ? "" : trecho.trim().toLowerCase();
        return usuarios.stream()
                .filter(u -> u.getEmail().toLowerCase().contains(alvo))
                .collect(Collectors.toList());
    }

    /** E-mails que contêm o trecho informado, em ordem alfabética (usa map + filter). */
    public List<String> buscarEmails(String trecho) {
        String alvo = trecho == null ? "" : trecho.trim().toLowerCase();
        return usuarios.stream()
                .map(Usuario::getEmail)
                .filter(email -> email.toLowerCase().contains(alvo))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------- Empréstimos

    /**
     * Empresta o livro (identificado pelo título) ao usuário (identificado pelo e-mail).
     *
     * @throws LivroIndisponivelException se o livro já estiver emprestado
     * @throws IllegalArgumentException   se o livro ou o usuário não existirem
     */
    public Emprestimo emprestar(String tituloLivro, String emailUsuario) throws LivroIndisponivelException {
        Livro livro = buscarLivroPorTitulo(tituloLivro)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado: " + tituloLivro + "."));
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + emailUsuario + "."));

        livro.emprestar();

        Emprestimo emprestimo = new Emprestimo(livro, usuario);
        historico.add(emprestimo);
        return emprestimo;
    }

    /**
     * Devolve o livro e fecha o registro correspondente no histórico.
     *
     * @throws IllegalArgumentException se o livro não existir
     * @throws IllegalStateException    se o livro não estiver emprestado
     */
    public Livro devolver(String tituloLivro) {
        Livro livro = buscarLivroPorTitulo(tituloLivro)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado: " + tituloLivro + "."));

        livro.devolver();

        historico.stream()
                .filter(Emprestimo::isEmAberto)
                .filter(e -> e.getLivro().getTitulo().equalsIgnoreCase(livro.getTitulo()))
                .findFirst()
                .ifPresent(Emprestimo::registrarDevolucao);
        return livro;
    }

    public List<Emprestimo> historicoDeEmprestimos() {
        return Collections.unmodifiableList(historico);
    }

    // ------------------------------------------------------------- Persistência

    /** Grava livros, usuários e histórico (nessa ordem) no arquivo .dat, por serialização. */
    public void salvar() throws IOException {
        ObjectOutputStream saida = null;
        try {
            saida = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(arquivo)));
            // Tudo vai no mesmo stream para que as referências compartilhadas
            // (o mesmo Livro nas listas e no histórico) sejam preservadas ao carregar.
            saida.writeObject(new ArrayList<>(livros));
            saida.writeObject(new ArrayList<>(usuarios));
            saida.writeObject(new ArrayList<>(historico));
            saida.flush();
        } catch (IOException e) {
            throw new IOException("Falha ao salvar os dados em " + arquivo + ": " + e.getMessage(), e);
        } finally {
            fechar(saida);
        }
    }

    /**
     * Carrega os dados do arquivo .dat.
     *
     * @return true se o arquivo existia e foi carregado; false se ainda não existe (biblioteca vazia)
     * @throws IOException se o arquivo existir mas estiver ilegível ou inválido
     */
    @SuppressWarnings("unchecked")
    public boolean carregar() throws IOException {
        if (!Files.exists(arquivo)) {
            return false;
        }
        ObjectInputStream entrada = null;
        try {
            entrada = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(arquivo)));
            List<Livro> livrosLidos = (List<Livro>) entrada.readObject();
            List<Usuario> usuariosLidos = (List<Usuario>) entrada.readObject();
            List<Emprestimo> historicoLido = (List<Emprestimo>) entrada.readObject();

            // Só altera o estado em memória depois de ler tudo com sucesso
            livros.clear();
            livros.addAll(livrosLidos);
            usuarios.clear();
            usuarios.addAll(usuariosLidos);
            historico.clear();
            historico.addAll(historicoLido);
            return true;
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Arquivo de dados inválido: " + arquivo, e);
        } catch (IOException e) {
            throw new IOException("Falha ao carregar os dados de " + arquivo + ": " + e.getMessage(), e);
        } finally {
            fechar(entrada);
        }
    }

    // ---------------------------------------------------------------- Auxiliares

    private static boolean vazio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private static void fechar(Closeable recurso) {
        if (recurso != null) {
            try {
                recurso.close();
            } catch (IOException ignorada) {
                // nada a fazer: os dados já foram gravados/lidos antes do fechamento
            }
        }
    }
}