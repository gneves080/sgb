import java.io.Serializable;

public class Livro implements Serializable, Comparable<Livro> {

    private static final long serialVersionUID = 1L;

    private final String titulo;
    private final String autor;
    private final int anoPublicacao;
    private boolean emprestado;

    public Livro(String titulo, String autor, int anoPublicacao) {
        this.titulo = titulo;
        this.autor = autor;
        this.anoPublicacao = anoPublicacao;
        this.emprestado = false;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public int getAnoPublicacao() {
        return anoPublicacao;
    }

    public boolean isEmprestado() {
        return emprestado;
    }

    public boolean isDisponivel() {
        return !emprestado;
    }

    /** Marca o livro como emprestado; falha se ele já estiver emprestado. */
    public void emprestar() throws LivroIndisponivelException {
        if (emprestado) {
            throw new LivroIndisponivelException(titulo);
        }
        emprestado = true;
    }

    /** Marca o livro como disponível; falha se ele não estiver emprestado. */
    public void devolver() {
        if (!emprestado) {
            throw new IllegalStateException("O livro \"" + titulo + "\" não está emprestado.");
        }
        emprestado = false;
    }

    /** Ordem natural: por título, sem diferenciar maiúsculas de minúsculas. */
    @Override
    public int compareTo(Livro outro) {
        return this.titulo.compareToIgnoreCase(outro.titulo);
    }

    @Override
    public String toString() {
        return String.format("\"%s\" - %s (%d) [%s]",
                titulo, autor, anoPublicacao, emprestado ? "Emprestado" : "Disponível");
    }
}