<?php

namespace App\Support;

use Rahim2797\MagicTypes\Properties;

class PropertySelectionService
{
    /**
     * @param Properties<\App\Models\Product> $properties
     */
    public function acceptProductProperties(array $properties): void
    {
    }

    /**
     * @return Properties<\App\Models\Product>|Properties<\App\Models\Account>
     */
    public function getProductOrAccountProperties(): array
    {
        return [];
    }
}
