/**
 * Lançada ao tentar emprestar um livro que já está emprestado.
 */
public class LivroIndisponivelException extends Exception {

    private static final long serialVersionUID = 1L;

    public LivroIndisponivelException(String titulo) {
        super("O livro \"" + titulo + "\" já está emprestado.");
    }
}