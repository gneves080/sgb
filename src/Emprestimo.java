import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro do histórico: quem pegou qual livro, e quando (e quando devolveu).
 */
public class Emprestimo implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Livro livro;
    private final Usuario usuario;
    private final LocalDateTime dataEmprestimo;
    private LocalDateTime dataDevolucao;

    public Emprestimo(Livro livro, Usuario usuario) {
        this.livro = livro;
        this.usuario = usuario;
        this.dataEmprestimo = LocalDateTime.now();
    }

    public Livro getLivro() {
        return livro;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getDataEmprestimo() {
        return dataEmprestimo;
    }

    public LocalDateTime getDataDevolucao() {
        return dataDevolucao;
    }

    public boolean isEmAberto() {
        return dataDevolucao == null;
    }

    public void registrarDevolucao() {
        this.dataDevolucao = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("\"%s\" | %s | emprestado em %s | %s",
                livro.getTitulo(), usuario, dataEmprestimo.format(FORMATO),
                dataDevolucao == null ? "ainda emprestado" : "devolvido em " + dataDevolucao.format(FORMATO));
    }
}