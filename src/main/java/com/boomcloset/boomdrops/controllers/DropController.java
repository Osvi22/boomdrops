package com.boomcloset.boomdrops.controllers;

import com.boomcloset.boomdrops.model.Drop;
import com.boomcloset.boomdrops.model.Item;
import com.boomcloset.boomdrops.repositories.DropRepository;
import com.boomcloset.boomdrops.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Controller
public class DropController {

    @Autowired
    private DropRepository dropRepository;

    @Autowired
    private ItemRepository itemRepository;

    @GetMapping("/drops")
    public String listarDrops(@RequestParam(required = false) String buscar, Model model) {
        var drops = (buscar != null && !buscar.isBlank())
                ? dropRepository.findByNombreContainingIgnoreCase(buscar)
                : dropRepository.findAll();

        double inversionTotalGeneral = 0;
        double valorInventarioGeneral = 0;
        double ventasRealesGeneral = 0;
        double gananciaPotencialGeneral = 0;
        double gananciaRealGeneral = 0;

        int disponiblesGeneral = 0;
        int vendidosGeneral = 0;

        Drop mejorDropRoi = null;
        double mejorRoiReal = -1;

        Map<String, Integer> topCategorias = new HashMap<>();
        Map<String, Integer> topMarcas = new HashMap<>();

        List<DropRankingDto> topDrops = new ArrayList<>();

        for (Drop drop : drops) {
            double inversionDrop = 0;
            double gananciaRealDrop = 0;
            int vendidosDrop = 0;
            int totalItemsDrop = 0;

            for (Item item : drop.getItems()) {
                inversionTotalGeneral += item.getCosto();
                gananciaPotencialGeneral += (item.getPrecio() - item.getCosto());
                inversionDrop += item.getCosto();
                totalItemsDrop++;

                String categoriaNormalizada = normalizarClave(item.getCategoria());
                String marcaNormalizada = normalizarClave(item.getMarca());

                if (!categoriaNormalizada.isBlank()) {
                    String categoriaMostrar = capitalizarTexto(categoriaNormalizada);
                    topCategorias.put(
                            categoriaMostrar,
                            topCategorias.getOrDefault(categoriaMostrar, 0) + 1
                    );
                }

                if (!marcaNormalizada.isBlank()) {
                    String marcaMostrar = capitalizarTexto(marcaNormalizada);
                    topMarcas.put(
                            marcaMostrar,
                            topMarcas.getOrDefault(marcaMostrar, 0) + 1
                    );
                }

                if ("VENDIDO".equals(item.getEstado())) {
                    vendidosGeneral++;
                    vendidosDrop++;
                    ventasRealesGeneral += item.getPrecio();
                    gananciaRealGeneral += (item.getPrecio() - item.getCosto());
                    gananciaRealDrop += (item.getPrecio() - item.getCosto());
                } else {
                    disponiblesGeneral++;
                    valorInventarioGeneral += item.getPrecio();
                }
            }

            double roiRealDrop = 0;
            double porcentajeVendidoDrop = 0;

            if (inversionDrop > 0) {
                roiRealDrop = (gananciaRealDrop / inversionDrop) * 100;
            }

            if (totalItemsDrop > 0) {
                porcentajeVendidoDrop = ((double) vendidosDrop / totalItemsDrop) * 100;
            }

            if (roiRealDrop > mejorRoiReal) {
                mejorRoiReal = roiRealDrop;
                mejorDropRoi = drop;
            }

            topDrops.add(new DropRankingDto(
                    drop.getId(),
                    drop.getNombre(),
                    drop.getFecha(),
                    roiRealDrop,
                    gananciaRealDrop,
                    porcentajeVendidoDrop
            ));
        }

        topDrops.sort((a, b) -> Double.compare(b.getRoiReal(), a.getRoiReal()));

        if (topDrops.size() > 5) {
            topDrops = new ArrayList<>(topDrops.subList(0, 5));
        }

        model.addAttribute("drops", drops);
        model.addAttribute("buscar", buscar);
        model.addAttribute("inversionTotalGeneral", inversionTotalGeneral);
        model.addAttribute("valorInventarioGeneral", valorInventarioGeneral);
        model.addAttribute("ventasRealesGeneral", ventasRealesGeneral);
        model.addAttribute("gananciaPotencialGeneral", gananciaPotencialGeneral);
        model.addAttribute("gananciaRealGeneral", gananciaRealGeneral);
        model.addAttribute("disponiblesGeneral", disponiblesGeneral);
        model.addAttribute("vendidosGeneral", vendidosGeneral);
        model.addAttribute("mejorDropRoi", mejorDropRoi);
        model.addAttribute("mejorRoiReal", mejorRoiReal);
        model.addAttribute("topDrops", topDrops);
        model.addAttribute("topCategorias", topCategorias);
        model.addAttribute("topMarcas", topMarcas);

        return "drops";
    }

    @PostMapping("/drops")
    public String guardarDrop(String nombre, LocalDate fecha) {
        Drop drop = new Drop();
        drop.setNombre(nombre);
        drop.setFecha(fecha);

        dropRepository.save(drop);

        return "redirect:/drops";
    }

    @GetMapping("/drops/{id}")
    public String verDrop(@PathVariable Long id, Model model) {
        Optional<Drop> dropOptional = dropRepository.findById(id);

        if (dropOptional.isEmpty()) {
            return "redirect:/drops";
        }

        Drop drop = dropOptional.get();

        drop.getItems().sort((item1, item2) -> {
            if (!item1.getEstado().equals(item2.getEstado())) {
                return item1.getEstado().equals("DISPONIBLE") ? -1 : 1;
            }
            return item1.getNombre().compareToIgnoreCase(item2.getNombre());
        });

        double inversionTotal = 0;
        double valorInventario = 0;
        double ventasReales = 0;
        double gananciaPotencial = 0;
        double gananciaReal = 0;

        int disponibles = 0;
        int vendidos = 0;

        for (Item item : drop.getItems()) {
            inversionTotal += item.getCosto();
            gananciaPotencial += (item.getPrecio() - item.getCosto());

            if ("VENDIDO".equals(item.getEstado())) {
                vendidos++;
                ventasReales += item.getPrecio();
                gananciaReal += (item.getPrecio() - item.getCosto());
            } else {
                disponibles++;
                valorInventario += item.getPrecio();
            }
        }

        double roiPotencial = 0;
        double roiReal = 0;
        double porcentajeVendido = 0;

        int totalItems = disponibles + vendidos;

        if (inversionTotal > 0) {
            roiPotencial = (gananciaPotencial / inversionTotal) * 100;
            roiReal = (gananciaReal / inversionTotal) * 100;
        }

        if (totalItems > 0) {
            porcentajeVendido = ((double) vendidos / totalItems) * 100;
        }

        model.addAttribute("drop", drop);
        model.addAttribute("inversionTotal", inversionTotal);
        model.addAttribute("valorInventario", valorInventario);
        model.addAttribute("ventasReales", ventasReales);
        model.addAttribute("gananciaPotencial", gananciaPotencial);
        model.addAttribute("gananciaReal", gananciaReal);
        model.addAttribute("disponibles", disponibles);
        model.addAttribute("vendidos", vendidos);
        model.addAttribute("roiPotencial", roiPotencial);
        model.addAttribute("roiReal", roiReal);
        model.addAttribute("porcentajeVendido", porcentajeVendido);

        return "drop-detail";
    }

    @PostMapping("/drops/{id}/items")
    public String guardarItem(
            @PathVariable Long id,
            String nombre,
            String categoria,
            String marca,
            double costo,
            double precio,
            String estado
    ) {
        Optional<Drop> dropOptional = dropRepository.findById(id);

        if (dropOptional.isEmpty()) {
            return "redirect:/drops";
        }

        Item item = new Item();
        item.setNombre(limpiarTexto(nombre));
        item.setCategoria(capitalizarTexto(categoria));
        item.setMarca(capitalizarTexto(marca));
        item.setCosto(costo);
        item.setPrecio(precio);
        item.setEstado(estado);
        item.setDrop(dropOptional.get());

        itemRepository.save(item);

        return "redirect:/drops/" + id;
    }

    @PostMapping("/items/{itemId}/toggle-estado")
    public String toggleEstadoItem(@PathVariable Long itemId) {
        Optional<Item> itemOptional = itemRepository.findById(itemId);

        if (itemOptional.isEmpty()) {
            return "redirect:/drops";
        }

        Item item = itemOptional.get();

        if ("DISPONIBLE".equals(item.getEstado())) {
            item.setEstado("VENDIDO");
            item.setFechaVenta(LocalDate.now());
        } else {
            item.setEstado("DISPONIBLE");
            item.setFechaVenta(null);
        }

        itemRepository.save(item);

        return "redirect:/drops/" + item.getDrop().getId();
    }

    @PostMapping("/items/{itemId}/eliminar")
    public String eliminarItem(@PathVariable Long itemId) {
        Optional<Item> itemOptional = itemRepository.findById(itemId);

        if (itemOptional.isEmpty()) {
            return "redirect:/drops";
        }

        Item item = itemOptional.get();
        Long dropId = item.getDrop().getId();

        itemRepository.delete(item);

        return "redirect:/drops/" + dropId;
    }

    @PostMapping("/drops/{id}/eliminar")
    public String eliminarDrop(@PathVariable Long id) {
        dropRepository.deleteById(id);
        return "redirect:/drops";
    }

    public static class DropRankingDto {
        private Long id;
        private String nombre;
        private LocalDate fecha;
        private double roiReal;
        private double gananciaReal;
        private double porcentajeVendido;

        public DropRankingDto(Long id, String nombre, LocalDate fecha, double roiReal, double gananciaReal, double porcentajeVendido) {
            this.id = id;
            this.nombre = nombre;
            this.fecha = fecha;
            this.roiReal = roiReal;
            this.gananciaReal = gananciaReal;
            this.porcentajeVendido = porcentajeVendido;
        }

        public Long getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public double getRoiReal() {
            return roiReal;
        }

        public double getGananciaReal() {
            return gananciaReal;
        }

        public double getPorcentajeVendido() {
            return porcentajeVendido;
        }
    }
    private String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim();
    }

    private String normalizarClave(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().toLowerCase();
    }

    private String capitalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        String limpio = texto.trim().toLowerCase();
        return limpio.substring(0, 1).toUpperCase() + limpio.substring(1);}

        @GetMapping("/items/{itemId}/editar")
public String mostrarFormularioEditarItem(@PathVariable Long itemId, Model model) {
    Optional<Item> itemOptional = itemRepository.findById(itemId);

    if (itemOptional.isEmpty()) {
        return "redirect:/drops";
    }

    Item item = itemOptional.get();
    model.addAttribute("item", item);

    return "edit-item";
}

@PostMapping("/items/{itemId}/editar")
public String actualizarItem(
        @PathVariable Long itemId,
        @RequestParam String nombre,
        @RequestParam String categoria,
        @RequestParam String marca,
        @RequestParam double costo,
        @RequestParam double precio,
        @RequestParam String estado
) {
    Optional<Item> itemOptional = itemRepository.findById(itemId);

    if (itemOptional.isEmpty()) {
        return "redirect:/drops";
    }

    Item item = itemOptional.get();

    item.setNombre(limpiarTexto(nombre));
    item.setCategoria(capitalizarTexto(categoria));
    item.setMarca(capitalizarTexto(marca));
    item.setCosto(costo);
    item.setPrecio(precio);
    item.setEstado(estado);

    if ("VENDIDO".equals(estado) && item.getFechaVenta() == null) {
        item.setFechaVenta(LocalDate.now());
    }

    if ("DISPONIBLE".equals(estado)) {
        item.setFechaVenta(null);
    }

    itemRepository.save(item);

    return "redirect:/drops/" + item.getDrop().getId();
    }
}