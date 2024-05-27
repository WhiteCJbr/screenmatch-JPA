package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=62f0a9c4&";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repository;
    private List<Serie> series = new ArrayList<>();

    public Principal(SerieRepository serieRepository) {
        this.repository = serieRepository;
    }


    public void exibeMenu() {


        var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar Series Buscadas
                4 - Buscar Serie por titulo
                5 - Buscar Series por ator
                6 - Top 5 Series
                7 - Buscar Series por categoria
                8 - Buscar Series por total de temporadas
                
                0 - Sair                                 
                """;
        var opcao = - 1;
        while (opcao != 0) {
            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTopSeries();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriesPorTotalTemporadas();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSeriesPorTotalTemporadas() {
        System.out.println("Quantas temporadas você deseja: ");
        var numTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Qual a avaliacao mínima: ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> series = repository.findAllByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(numTemporadas, avaliacao);
        series.forEach(s -> System.out.println(s.getTitulo() + ", Total de Temporadas: " + s.getTotalTemporadas() + ", Avaliacao: " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Deseja buscar séries de que categoria / genero");
        var nomeGenero = leitura.nextLine();

        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repository.findByGenero(categoria);
        System.out.println("Séries da categoria " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarTopSeries() {
        List<Serie> serieTop = repository.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach(s -> System.out.println(s.getTitulo() + ": " + s.getAvaliacao()));;
    }

    private void buscarSeriePorAtor() {
        System.out.println("Qual o nome para busca:");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliações a partir de que valor: ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repository.findByAtorContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Series em que " + nomeAtor + " trabalhou:");
        seriesEncontradas.forEach(s -> System.out.println(s.getTitulo() + ": " + s.getAvaliacao()));
    }

    private void buscarSeriePorTitulo() {

        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serieBuscada = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBuscada.isPresent()) {
            System.out.println("Dados da série: " + serieBuscada.get());
        }else{
            System.out.println("Série não encontrada!");
        }

    }

    private void listarSeriesBuscadas() {
        this.series = repository.findAll();
        this.series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);

    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        //this.dadosSeries.add(dados);
        repository.save(serie);


    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios =  temporadas.stream()
                                        .flatMap(d -> d.episodios().stream()
                                                .map(e -> new Episodio(d.numero(), e)))
                                        .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);
        }else{
            System.out.println("Serie não encontrada");
        }

    }
}