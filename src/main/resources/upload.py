import click.core
import pros.common.ui as ui
import pros.conductor as c
import sys

def resolve_v5_port(port, type, quiet = False):
    from pros.serial.devices.vex import find_v5_ports
    if not port:
        ports = find_v5_ports(type)
        if len(ports) == 0:
            if not quiet:
                print('No v5 ports were found!')
            return None
        if len(ports) > 1:
            if not quiet:
                port = click.prompt('Multiple v5 ports were found. Please choose one: ',
                                    default=ports[0].device,
                                    type=click.Choice([p.device for p in ports]))
                assert port in [p.device for p in ports]
            else:
                return None
        else:
            port = ports[0].device
            print('Automatically selected {}'.format(port))
    return port

def upload(path, name, desc, slot, icon):
    """
    Upload binary file to v5 Brain
    """
    import pros.serial.devices.vex as vex
    from pros.serial.ports import DirectPort

    port = resolve_v5_port(None, 'system')

    kwargs = {}

    if name is None:
        kwargs['remote_name'] = os.path.splitext(os.path.basename(path))[0]
    else:
        kwargs['remote_name'] = name.replace('@', '_')
    kwargs['slot'] = slot - 1
    kwargs['run_after'] = vex.V5Device.FTCompleteOptions.RUN_SCREEN
    kwargs['compress_bin'] = False
    kwargs['description'] = desc
    if icon is None:
        kwargs['icon'] = 'USER921x.bmp'
    else:
        kwargs['icon'] = icon

    if port == None:
        print('Couldn\'t find port, aborting')
        exit(1)
    else:
        print('Arguments: {}'.format(str(kwargs)))
        print('Port: {}'.format(str(port)))
        # Do the actual uploading!
        try:
            ser = DirectPort(port)
            device = device = vex.V5Device(ser)
            with click.open_file(path, mode = 'rb') as pf:
                device.write_program(pf, **kwargs)
        except Exception as e:
            print(e)
            exit(1)

def ls_usb():
    """
    List plugged in VEX Devices
    """
    from pros.serial.devices.vex import find_v5_ports, find_cortex_ports

    class PortReport(object):
        def __init__(self, header: str, ports, machine_header: str = None):
            self.header = header
            self.ports = [{'device': p.device, 'desc': p.description} for p in ports]
            self.machine_header = machine_header or header

        def __getstate__(self):
            return {
                'device_type': self.machine_header,
                'devices': self.ports
            }

        def __str__(self):
            if len(self.ports) == 0:
                return f'There are no connected {self.header}'
            else:
                port_str = "\n".join([f"{p['device']} - {p['desc']}" for p in self.ports])
                return f'{self.header}:\n{port_str}'

    result = []
    ports = find_v5_ports('system')
    result.append(PortReport('VEX EDR V5 System Ports', ports, 'v5/system'))

    ports = find_v5_ports('User')
    result.append(PortReport('VEX EDR V5 User ports', ports, 'v5/user'))

    ui.finalize('lsusb', result)

#ls_usb()
upload(sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4]), sys.argv[5])