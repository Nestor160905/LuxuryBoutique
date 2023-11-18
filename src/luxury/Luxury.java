package luxury;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Luxury extends JFrame {
    private final JTextField txtNombre;
    private final JTextField txtPrecio;
    private final JTextField txtStock;
    private final JTextField txtDescripcion;
    private final JTextField txtImagen;

    private final JTable tablaProductos;
    private final DefaultTableModel modeloTabla;
    private final JLabel lblImagen;

    private final Connection conexion;

    public Luxury(Connection conexion) {
        this.conexion = conexion;

        // Configuración de la interfaz gráfica
        setTitle("Luxury WM Boutique");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel superior con la entrada de datos
        JPanel panelEntrada = new JPanel(new GridLayout(8, 2));

        panelEntrada.add(new JLabel("Nombre:"));
        txtNombre = new JTextField();
        panelEntrada.add(txtNombre);

        panelEntrada.add(new JLabel("Precio:"));
        txtPrecio = new JTextField();
        panelEntrada.add(txtPrecio);

        panelEntrada.add(new JLabel("Stock:"));
        txtStock = new JTextField();
        panelEntrada.add(txtStock);

        panelEntrada.add(new JLabel("Descripción:"));
        txtDescripcion = new JTextField();
        panelEntrada.add(txtDescripcion);

        panelEntrada.add(new JLabel("Imagen:"));
        txtImagen = new JTextField();
        panelEntrada.add(txtImagen);

        JButton btnCargarImagen = new JButton("Cargar Imagen");
        btnCargarImagen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarImagen();
            }
        });
        panelEntrada.add(btnCargarImagen);

        JButton btnAgregar = new JButton("Agregar Producto");
        btnAgregar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                agregarProducto();
            }
        });
        panelEntrada.add(btnAgregar);

        // Panel central con la tabla de productos
        modeloTabla = new DefaultTableModel();
        modeloTabla.addColumn("ID");
        modeloTabla.addColumn("Nombre");
        modeloTabla.addColumn("Precio");
        modeloTabla.addColumn("Stock");
        modeloTabla.addColumn("Descripción");
        modeloTabla.addColumn("Fecha de Subida");

        tablaProductos = new JTable(modeloTabla);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProductos.getSelectionModel().addListSelectionListener(e -> {
            int filaSeleccionada = tablaProductos.getSelectedRow();
            if (filaSeleccionada != -1) {
                mostrarImagen(filaSeleccionada);
            }
        });

        JScrollPane scrollPane = new JScrollPane(tablaProductos);

        // Panel derecho para mostrar la imagen del producto seleccionado
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblImagen = new JLabel();
        panelDerecho.add(lblImagen, BorderLayout.CENTER);

        // Añadir paneles a la ventana
        add(panelEntrada, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(panelDerecho, BorderLayout.EAST);

        // Deshabilitar la interfaz hasta que se ingrese correctamente
        deshabilitarInterfaz();

        // Mostrar productos al inicio
        mostrarProductos();
    }

    private void cargarImagen() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "gif"));
        fileChooser.showOpenDialog(this);
        File file = fileChooser.getSelectedFile();
        if (file != null) {
            txtImagen.setText(file.getAbsolutePath());
            mostrarImagen(file);
        }
    }

    private void agregarProducto() {
        try {
            // Obtener datos de la interfaz
            String nombre = txtNombre.getText();
            int precio = 0;
            int stock = 0;

            try {
                precio = Integer.parseInt(txtPrecio.getText());
                stock = Integer.parseInt(txtStock.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ingresa valores numéricos para Precio y Stock.");
                return;
            }

            String descripcion = txtDescripcion.getText();

            // Obtener la fecha y hora actual del sistema
            Date fechaActual = new Date();

            // Formatear la fecha al formato deseado (yyyy-MM-dd HH:mm:ss)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fechaSubida = dateFormat.format(fechaActual);

            String imagenPath = txtImagen.getText();

            // Leer la imagen como un conjunto de bytes
            byte[] imagenBytes = null;
            try (FileInputStream fis = new FileInputStream(new File(imagenPath))) {
                imagenBytes = fis.readAllBytes();
            } catch (IOException ex) {
                Logger.getLogger(Luxury.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Insertar datos en la base de datos
            String query = "INSERT INTO productos (nombre, precio, stock, descripcion, fecha_subida, imagen) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = conexion.prepareStatement(query)) {
                statement.setString(1, nombre);
                statement.setInt(2, precio);
                statement.setInt(3, stock);
                statement.setString(4, descripcion);
                statement.setString(5, fechaSubida);
                statement.setBytes(6, imagenBytes);

                statement.executeUpdate();
            }

            // Actualizar la tabla de productos
            mostrarProductos();

            // Limpiar campos después de agregar el producto
            limpiarCampos();

        } catch (SQLException ex) {
            Logger.getLogger(Luxury.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void mostrarProductos() {
        try {
            // Obtener datos de la base de datos
            String query = "SELECT * FROM productos";

            try (Statement statement = conexion.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                // Limpiar la tabla antes de agregar nuevos datos
                modeloTabla.setRowCount(0);

                // Mostrar productos en la tabla
                while (resultSet.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(resultSet.getInt("id"));
                    fila.add(resultSet.getString("nombre"));
                    fila.add(resultSet.getInt("precio"));
                    fila.add(resultSet.getInt("stock"));
                    fila.add(resultSet.getString("descripcion"));
                    fila.add(resultSet.getString("fecha_subida"));
                    modeloTabla.addRow(fila);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(Luxury.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void mostrarImagen(File file) {
        ImageIcon imagen = new ImageIcon(file.getAbsolutePath());
        Image img = imagen.getImage();
        Image imgEscalada = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
        ImageIcon imagenEscalada = new ImageIcon(imgEscalada);
        lblImagen.setIcon(imagenEscalada);
    }

    private void mostrarImagen(int fila) {
        try {
            int idProducto = (int) tablaProductos.getValueAt(fila, 0);

            // Obtener la imagen del producto desde la base de datos
            String query = "SELECT imagen FROM productos WHERE id = ?";
            try (PreparedStatement statement = conexion.prepareStatement(query)) {
                statement.setInt(1, idProducto);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        byte[] imagenBytes = resultSet.getBytes("imagen");
                        ImageIcon imagen = new ImageIcon(imagenBytes);
                        Image img = imagen.getImage();
                        Image imgEscalada = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                        ImageIcon imagenEscalada = new ImageIcon(imgEscalada);
                        lblImagen.setIcon(imagenEscalada);
                    }
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(Luxury.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void limpiarCampos() {
        // Limpiar los campos de entrada después de agregar un producto
        txtNombre.setText("");
        txtPrecio.setText("");
        txtStock.setText("");
        txtDescripcion.setText("");
        txtImagen.setText("");
        lblImagen.setIcon(null);
    }

    private void deshabilitarInterfaz() {
        // Deshabilitar la interfaz hasta que se ingrese correctamente
        for (Component component : getContentPane().getComponents()) {
            component.setEnabled(false);
        }
    }

    public static void main(String[] args) {
        try {
            // Establecer la conexión a la base de datos
            String url = "jdbc:mysql://localhost:3306/luxury";
            String user = "root";
            String password = "";
            Connection conexion = DriverManager.getConnection(url, user, password);

            // Cambiar el estilo de la interfaz a Nimbus
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                Logger.getLogger(Luxury.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Crear la ventana del sistema
            Luxury sistema = new Luxury(conexion);

            // Configurar la visibilidad de la ventana
            sistema.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(Luxury.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "No se pudo establecer la conexión a la base de datos");
        }
    }
}
