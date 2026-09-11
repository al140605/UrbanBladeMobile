package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.ProductItem
import com.urbanblade.mobile.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(user: AuthUser, onOrders: () -> Unit, vm: StoreViewModel = viewModel()) {
    val products by vm.products.collectAsState(); val cart by vm.cart.collectAsState(); val busy by vm.busy.collectAsState(); val error by vm.error.collectAsState(); val message by vm.message.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.load() }
    val total = products.sumOf { it.precioVenta * (cart[it.id] ?: 0) }
    val isClient = user.roles.contains("cliente")
    Scaffold(
        topBar={TopAppBar(title={Text("Tienda")},actions={IconButton(onClick=onOrders){Icon(Icons.Default.ReceiptLong,"Pedidos")}})},
        bottomBar={
            if (cart.isNotEmpty() && isClient) Surface(tonalElevation=6.dp,shadowElevation=8.dp){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Column(Modifier.weight(1f)){Text("${cart.values.sum()} artículos",fontWeight=FontWeight.Bold);Text("$${"%.2f".format(total)} MXN")}
                    Button(onClick={vm.checkout(onOrders)},enabled=!busy){Text("Crear pedido")}
                }
            }
        }
    ){p->
        LazyColumn(Modifier.fillMaxSize().padding(p).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)){
            item{OutlinedTextField(query,{query=it},label={Text("Buscar productos")},modifier=Modifier.fillMaxWidth(),singleLine=true,trailingIcon={IconButton(onClick={vm.load(query.ifBlank{null})}){Icon(Icons.Default.Search,"Buscar")}})}
            if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            error?.let{item{ErrorCard(it)}};message?.let{item{InfoCard(it)}}
            if(!isClient)item{InfoCard("Puedes consultar el catálogo. El checkout está disponible para cuentas con rol cliente.")}
            items(products,key={it.id}){product->ProductCard(product,cart[product.id]?:0,{if(isClient)vm.add(product.id)},{if(isClient)vm.remove(product.id)})}
            if(products.isEmpty()&&!busy)item{EmptyState("No hay productos disponibles.")}
        }
    }
}

@Composable
private fun ProductCard(item: ProductItem, qty:Int, add:()->Unit, remove:()->Unit){
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(item.nombre,fontWeight=FontWeight.Bold); item.categoria?.let{Text(it,style=MaterialTheme.typography.labelMedium)}}; Text("$${"%.2f".format(item.precioVenta)}",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}
        item.descripcion?.takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Stock: ${item.stockActual}",Modifier.weight(1f)); if(qty>0) IconButton(onClick=remove){Icon(Icons.Default.Remove,"Quitar")}; if(qty>0) Text(qty.toString()); IconButton(onClick=add,enabled=qty<item.stockActual){Icon(Icons.Default.Add,"Agregar")}}
    }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(user:AuthUser,onBack:()->Unit,vm:OrdersViewModel=viewModel()){
    val data by vm.data.collectAsState(); val busy by vm.busy.collectAsState(); val error by vm.error.collectAsState(); val staff=user.roles.any{it=="administrador"||it=="recepcionista"}
    LaunchedEffect(Unit){vm.load()}
    Scaffold(topBar={TopAppBar(title={Text("Pedidos")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver")}},actions={IconButton(onClick={vm.load()}){Icon(Icons.Default.Refresh,"Actualizar")}})}){p->
        LazyColumn(Modifier.fillMaxSize().padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}; error?.let{item{ErrorCard(it)}}
            items(data.data,key={it.id}){o->OrderCard(o,staff,{vm.cancel(o.id)},{m->vm.deliver(o.id,m)})}
            if(data.data.isEmpty()&&!busy)item{EmptyState("Aún no hay pedidos.")}
        }
    }
}

@Composable
private fun OrderCard(o:OrderRow,staff:Boolean,cancel:()->Unit,deliver:(String)->Unit){
    var menu by remember{mutableStateOf(false)}
    ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(o.folio?:"Pedido",fontWeight=FontWeight.Bold); AssistChip(onClick={},label={Text(o.estado)})}
        o.client?.name?.let{Text("Cliente: $it")}; Text("Total: $${"%.2f".format(o.total)} MXN",fontWeight=FontWeight.SemiBold)
        o.items.forEach{Text("• ${it.cantidad} × ${it.nombre ?: "Producto"} — $${"%.2f".format(it.subtotal)}",style=MaterialTheme.typography.bodySmall)}
        if(o.estado=="pendiente") Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=cancel){Text("Cancelar")}; if(staff){Box{Button(onClick={menu=true}){Text("Entregar")};DropdownMenu(menu,{menu=false}){listOf("efectivo","tarjeta","transferencia").forEach{m->DropdownMenuItem(text={Text(m.replaceFirstChar(Char::uppercase))},onClick={menu=false;deliver(m)})}}}}}
    }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(user:AuthUser,onBack:()->Unit,vm:PaymentsViewModel=viewModel()){
    val staff=user.roles.any{it=="administrador"||it=="recepcionista"}; val payments by vm.payments.collectAsState(); val pending by vm.pending.collectAsState(); val busy by vm.busy.collectAsState(); val error by vm.error.collectAsState()
    LaunchedEffect(Unit){vm.load(staff)}
    Scaffold(topBar={TopAppBar(title={Text(if(staff)"Pagos y comprobantes" else "Mis pagos")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver")}})}){p->
        LazyColumn(Modifier.fillMaxSize().padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}; error?.let{item{ErrorCard(it)}}
            if(staff&&pending.data.isNotEmpty()){item{SectionTitle("Transferencias por revisar")};items(pending.data,key={it.id}){x->ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("$${"%.2f".format(x.monto)}",fontWeight=FontWeight.Bold);x.ocrTexto?.let{Text(it,style=MaterialTheme.typography.bodySmall)};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={vm.approve(x.id,staff)}){Text("Aprobar")};OutlinedButton(onClick={vm.reject(x.id,"Comprobante no válido",staff)}){Text("Rechazar")}}}}}}
            item{SectionTitle("Historial")};items(payments.data,key={it.id}){x->ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("$${"%.2f".format(x.monto+x.propina)} MXN",fontWeight=FontWeight.Bold);Text(x.metodoPago?:"Pago");x.appointment?.let{a->Text(listOfNotNull(a.service,a.barber,a.fecha).joinToString(" · "),style=MaterialTheme.typography.bodySmall)}}}}
            if(payments.data.isEmpty()&&!busy)item{EmptyState("No hay pagos registrados.")}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack:()->Unit,vm:NotificationsViewModel=viewModel()){
    val data by vm.data.collectAsState();val busy by vm.busy.collectAsState();val error by vm.error.collectAsState();LaunchedEffect(Unit){vm.load()}
    Scaffold(topBar={TopAppBar(title={Text("Notificaciones")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver")}},actions={TextButton(onClick={vm.readAll()}){Text("Leer todas")}})}){p->JsonContentScreen(data,busy,error,Modifier.padding(p))}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericModuleScreen(title:String,endpoint:String,onBack:()->Unit,vm:GenericModuleViewModel=viewModel()){
    val data by vm.data.collectAsState();val busy by vm.busy.collectAsState();val error by vm.error.collectAsState();LaunchedEffect(endpoint){vm.load(endpoint)}
    Scaffold(topBar={TopAppBar(title={Text(title)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver")}},actions={IconButton(onClick={vm.load(endpoint)}){Icon(Icons.Default.Refresh,"Actualizar")}})}){p->JsonContentScreen(data,busy,error,Modifier.padding(p))}
}

@Composable
private fun JsonContentScreen(data:JsonObject?,busy:Boolean,error:String?,modifier:Modifier=Modifier){
    LazyColumn(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())};error?.let{item{ErrorCard(it)}}
        data?.entrySet()?.forEach{(k,v)->item{JsonCard(k,v)}}
        if(data==null&&!busy&&error==null)item{EmptyState("Sin información.")}
    }
}

@Composable
private fun JsonCard(label:String,value:JsonElement){
    ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(label.replace('_',' ').replaceFirstChar(Char::uppercase),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary); when{
        value.isJsonArray-> value.asJsonArray.take(20).forEachIndexed{i,e-> Text("${i+1}. ${compactJson(e)}",style=MaterialTheme.typography.bodySmall)}
        value.isJsonObject-> value.asJsonObject.entrySet().take(20).forEach{(k,v)->Text("${k.replace('_',' ')}: ${compactJson(v)}",style=MaterialTheme.typography.bodySmall)}
        else->Text(compactJson(value))
    }}}
}
private fun compactJson(e:JsonElement):String = when{e.isJsonNull->"—";e.isJsonPrimitive->e.asJsonPrimitive.toString().trim('"');e.isJsonObject->e.asJsonObject.entrySet().take(5).joinToString(" · "){"${it.key}: ${compactJson(it.value)}"};e.isJsonArray->"${e.asJsonArray.size()} elementos";else->e.toString()}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(onBack:()->Unit,vm:ChatbotViewModel=viewModel()){
    val messages by vm.messages.collectAsState();val busy by vm.busy.collectAsState();var text by remember{mutableStateOf("")}
    Scaffold(topBar={TopAppBar(title={Text("Bladebot")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver")}})},bottomBar={Surface(shadowElevation=8.dp){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(text,{text=it},Modifier.weight(1f),placeholder={Text("Escribe tu pregunta")});IconButton(onClick={ val t=text; text=""; vm.send(t) },enabled=text.isNotBlank()&&!busy){Icon(Icons.Default.Send,"Enviar")}}}}){p->
        LazyColumn(Modifier.fillMaxSize().padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(messages){m->Box(Modifier.fillMaxWidth()){Surface(tonalElevation=2.dp,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth(if(m.first)0.85f else 1f)){Text(m.second,Modifier.padding(12.dp))}}};if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}}
    }
}

@Composable
fun MoreScreen(user:AuthUser,onNavigate:(String)->Unit){
    val roles=user.roles.toSet();val staff=roles.any{it=="administrador"||it=="recepcionista"};val admin="administrador" in roles;val engineer="ingeniero" in roles;val barber="barbero" in roles;val client="cliente" in roles
    val modules=buildList{
        add(ModuleItem("catalog","Servicios y barberos",Icons.Default.ContentCut));add(ModuleItem("notifications","Notificaciones",Icons.Default.Notifications));add(ModuleItem("analytics","Analítica",Icons.Default.QueryStats));add(ModuleItem("social","Muro social",Icons.Default.Groups));add(ModuleItem("chatbot","Bladebot",Icons.Default.SmartToy)); if(staff||client){add(ModuleItem("payments","Pagos",Icons.Default.Payments));add(ModuleItem("orders","Pedidos",Icons.Default.ReceiptLong))}
        if(staff){add(ModuleItem("clients","Clientes",Icons.Default.People));add(ModuleItem("inventory","Inventario",Icons.Default.Inventory2));add(ModuleItem("cash","Corte de caja",Icons.Default.PointOfSale))}
        if(barber){add(ModuleItem("barber_agenda","Mi agenda",Icons.Default.Event));add(ModuleItem("barber_portfolio","Portafolio",Icons.Default.PhotoLibrary));add(ModuleItem("barber_schedule","Mi horario",Icons.Default.Schedule))}
        if(admin||engineer){add(ModuleItem("reports","Reportes",Icons.Default.Assessment));add(ModuleItem("logs","Logs",Icons.Default.Terminal));add(ModuleItem("admin_metrics","Métricas",Icons.Default.MonitorHeart));add(ModuleItem("insights","Insights IA",Icons.Default.AutoAwesome))}
        if(admin){add(ModuleItem("campaigns","Campañas",Icons.Default.Campaign));add(ModuleItem("raffles","Sorteos",Icons.Default.EmojiEvents));add(ModuleItem("reviews","Reseñas",Icons.Default.Star));add(ModuleItem("users","Usuarios",Icons.Default.ManageAccounts));add(ModuleItem("settings","Configuración",Icons.Default.Settings))}
        if(engineer||admin){add(ModuleItem("system","Estado del sistema",Icons.Default.Dns))}
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Más módulos",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Opciones disponibles según tus roles: ${user.roles.joinToString()}",style=MaterialTheme.typography.bodySmall)};items(modules){m->ElevatedCard(onClick={onNavigate(m.route)},modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(m.icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(14.dp));Text(m.title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Icon(Icons.Default.ChevronRight,null)}}}}
}
private data class ModuleItem(val route:String,val title:String,val icon:ImageVector)

@Composable private fun SectionTitle(t:String)=Text(t,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
@Composable private fun ErrorCard(t:String)=Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)){Text(t,Modifier.padding(12.dp),color=MaterialTheme.colorScheme.onErrorContainer)}
@Composable private fun InfoCard(t:String)=Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)){Text(t,Modifier.padding(12.dp))}
@Composable private fun EmptyState(t:String)=Box(Modifier.fillMaxWidth().padding(28.dp),contentAlignment=Alignment.Center){Text(t,color=MaterialTheme.colorScheme.onSurfaceVariant)}
